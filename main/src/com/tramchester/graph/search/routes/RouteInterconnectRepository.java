package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RouteInterconnectRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterconnectRepository.class);

    private final RouteIndex routeIndex;
    private final RouteDateAndDayOverlap routeDateAndDayOverlap;
    private final RouteIndexPairFactory pairFactory;
    private final int numRoutes;
    private final InterchangeRepository interchangeRepository;
    private final RouteCostMatrix routeCostMatrix;

    private final List<RouteInterconnects> links;

    @Inject
    public RouteInterconnectRepository(RouteIndexPairFactory pairFactory, NumberOfRoutes numberOfRoutes, RouteIndex routeIndex,
                                       InterchangeRepository interchangeRepository, RouteCostMatrix routeCostMatrix,
                                       RouteDateAndDayOverlap routeDateAndDayOverlap) {
        this.pairFactory = pairFactory;
        this.numRoutes = numberOfRoutes.numberOfRoutes();
        this.interchangeRepository = interchangeRepository;
        this.routeCostMatrix = routeCostMatrix;
        this.routeIndex = routeIndex;
        this.routeDateAndDayOverlap = routeDateAndDayOverlap;
        links = new ArrayList<>(RouteCostMatrix.MAX_DEPTH);
    }

    @PostConstruct
    public void start() {
        for (int depth = 0; depth < RouteCostMatrix.MAX_DEPTH; depth++) {
            RouteInterconnects routeInterconnects = new RouteInterconnects(pairFactory, numRoutes);
            links.add(routeInterconnects);
        }
        createBacktracking(routeDateAndDayOverlap);
    }

    @PreDestroy
    public void clear() {
        links.clear();
    }

    private void createBacktracking(final RouteDateAndDayOverlap routeDateAndDayOverlap) {
        // degree 1 = depth 0 = interchanges directly
        for (int currentDegree = 1; currentDegree <= RouteCostMatrix.MAX_DEPTH; currentDegree++) {
            createBacktracking(routeDateAndDayOverlap, currentDegree);
        }
    }

    private void createBacktracking(final RouteDateAndDayOverlap routeDateAndDayOverlap, final int currentDegree) {
        final int totalSize = numRoutes * numRoutes;
        if (currentDegree<1) {
            throw new RuntimeException("Only call for >1 , got " + currentDegree);
        }

        final ImmutableIndexedBitSet matrixForDegree = routeCostMatrix.getDegree(currentDegree);

        logger.info("Create backtrack pair map for degree " + currentDegree + " matrixForDegree bits set " + matrixForDegree.numberOfBitsSet());

        if (matrixForDegree.numberOfBitsSet()>0) {
            // zero indexed
            final RouteInterconnects routeInterconnects = forDegree(currentDegree);

            final Instant startTime = Instant.now();

            for (short currentRoute = 0; currentRoute < numRoutes; currentRoute++) {
                final short currentRouteIndex = currentRoute;

                final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForRoute = routeDateAndDayOverlap.overlapsFor(currentRouteIndex);

                final SimpleImmutableBitmap currentConnections = matrixForDegree.getBitSetForRow(currentRouteIndex);

                currentConnections.getBitIndexes().
                        filter(dateOverlapsForRoute::get). // true if route runs on date
                        forEach(connectedRoute -> {
                    final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForConnectedRoute = routeDateAndDayOverlap.overlapsFor(connectedRoute);
                    final SimpleImmutableBitmap intermediates = matrixForDegree.getBitSetForRow(connectedRoute);
                    routeInterconnects.addLinksBetween(currentRouteIndex, connectedRoute, intermediates,
                            dateOverlapsForRoute, dateOverlapsForConnectedRoute);
                });
            }

            final long took = Duration.between(startTime, Instant.now()).toMillis();
            final int added = routeInterconnects.numberOfLinks();
            final double percentage = ((double)added)/((double)totalSize) * 100D;
            logger.info(String.format("Added backtrack pairs %s (%s %%) Degree %s in %s ms",
                    added, percentage, currentDegree, took));

        } else {
            logger.info("No bits set for degree " + currentDegree);
        }
    }

    private void guardDepth(final int depth) {
        if (depth < 0 || depth > RouteCostMatrix.MAX_DEPTH) {
            String message = "Depth:" + depth + " is out of range";
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    private Stream<Pair<RouteIndexPair, RouteIndexPair>> forDepth(final int depth, final RouteIndexPair indexPair) {
        guardDepth(depth);
        if (links.get(depth).hasLinksFor(indexPair)) {
            return links.get(depth).getLinksFor(indexPair);
        }

        final short degree = routeCostMatrix.getDegree(indexPair);
        final RoutePair missing = routeIndex.getPairFor(indexPair);
        final String message = MessageFormat.format("Missing indexPair {0} ({1}) at depth {2} actual degree for pair {3}",
                indexPair, missing, depth, degree);
        logger.error(message);
        if (!missing.isDateOverlap()) {
            logger.warn("Also no date overlap between " + missing);
        }
        if (!interchangeRepository.hasInterchangeFor(indexPair)) {
            logger.warn("Also no interchange for " + missing);
        }
        throw new RuntimeException(message);
    }

    private RouteInterconnects forDegree(final int currentDegree) {
        return forDepth(currentDegree - 1);
    }

    private RouteInterconnects forDepth(final int depth) {
        guardDepth(depth);
        return links.get(depth);
    }

    private Stream<Pair<RouteIndexPair, RouteIndexPair>> forDegree(final int degree, final RouteIndexPair indexPair) {
        return forDepth(degree - 1, indexPair);
    }

    // test support
    public Set<Pair<RoutePair, RoutePair>> getBackTracksFor(int degree, RouteIndexPair indexPair) {
        return forDegree(degree, indexPair).
                map(pair -> Pair.of(routeIndex.getPairFor(pair.getLeft()), routeIndex.getPairFor(pair.getRight()))).
                collect(Collectors.toSet());
    }

    // test support
    public int getNumberBacktrackFor(final int depth) {
        return forDegree(depth).numberOfLinks();
    }

    /***
     * Finds the paths (between the routes, via interchanges) between the routes given in the indexPair, for the given
     * date overlaps between the 2, filtering the interchanges using the profided filter.
     * @param indexPair the routes to find path between
     * @param dateOverlaps representation of the dates the two routes overlap (aka run) on
     * @param interchangeFilter an (inclusive) filter for the interchanges to use when finding the path
     * @return paths between the 2 routes from the indexPair
     */
    public PathResults getInterchangesFor(final RouteIndexPair indexPair, final ImmutableIndexedBitSet dateOverlaps,
                                          final Function<InterchangeStation, Boolean> interchangeFilter) {
        final int degree = routeCostMatrix.getDegree(indexPair);

        if (degree==Byte.MAX_VALUE) {
            logger.warn("No degree found for " + routeIndex.getPairFor(indexPair));
            return new PathResults.NoPathResults();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Get interchanges for %s with initial degree %s", HasId.asIds(routeIndex.getPairFor(indexPair)), degree));
        }

        final ImmutableIndexedBitSet changesForDegree = routeCostMatrix.getDegree(degree).getCopyOfRowAndColumn(indexPair.first(), indexPair.second());
        // apply mask to filter out unavailable dates/modes quickly
        final IndexedBitSet withDateApplied = IndexedBitSet.and(changesForDegree, dateOverlaps);

        if (withDateApplied.isSet(indexPair)) {

            QueryPathsWithDepth.QueryPath pathFor = getPathFor(indexPair, degree, interchangeFilter);
            if (pathFor.hasAny()) {
                return new PathResults.HasPathResults(pathFor);
            } else {
                return new PathResults.NoPathResults();
            }

        } else {
            return new PathResults.NoPathResults();
        }
    }

    private QueryPathsWithDepth.QueryPath getPathFor(final RouteIndexPair indexPair, final int currentDegree,
                                                     final Function<InterchangeStation, Boolean> interchangeFilter) {

        if (currentDegree==1) {
            if (!interchangeRepository.hasInterchangeFor(indexPair)) {
                final RoutePair routePair = routeIndex.getPairFor(indexPair);
                final String msg = "Unable to find interchange for " + HasId.asIds(routePair);
                logger.error(msg);
                logger.warn("Full pair first:" + routePair.first() + " second: " + routePair.second());
                if (!routePair.isDateOverlap()) {
                    logger.error("Further: No date overlap between " + HasId.asIds(routePair));
                }
                throw new RuntimeException(msg);
            }

            // can be multiple interchanges points between a pair of routes
            final Set<InterchangeStation> changes = interchangeRepository.getInterchangesFor(indexPair).
                    filter(interchangeFilter::apply).
                    collect(Collectors.toSet());

            if (changes.isEmpty()) {
                return QueryPathsWithDepth.ZeroPaths.get();
            } else {
                return QueryPathsWithDepth.AnyOfInterchanges.Of(changes);
            }
        } else {
            final int lowerDegree = currentDegree-1;
            //final int depth = degree - 1;

            final Stream<Pair<RouteIndexPair, RouteIndexPair>> underlying = forDegree(lowerDegree, indexPair);
            // TODO parallel? not required?
            final Set<QueryPathsWithDepth.BothOf> combined = underlying. //.parallel().
                    map(pair -> expandPathFor(pair, lowerDegree, interchangeFilter)).
                    filter(QueryPathsWithDepth.BothOf::hasAny).
                    collect(Collectors.toSet());

            return new QueryPathsWithDepth.AnyOf(combined);
        }
    }

    @NotNull
    private QueryPathsWithDepth.BothOf expandPathFor(final Pair<RouteIndexPair, RouteIndexPair> pair, final int degree,
                                                     final Function<InterchangeStation, Boolean> interchangeFilter) {
        final QueryPathsWithDepth.QueryPath pathA = getPathFor(pair.getLeft(), degree, interchangeFilter);
        final QueryPathsWithDepth.QueryPath pathB = getPathFor(pair.getRight(), degree, interchangeFilter);
        return new QueryPathsWithDepth.BothOf(pathA, pathB);
    }


    private static class RouteInterconnects {
        private final RouteIndexPairFactory pairFactory;

        // (A, C) -> (A, B) (B ,C)
        // reduces to => (A,C) -> B

        // pair to connecting route index (A,B) -> [C]
        private final Map<Integer, BitSet> bitSetForIndex;
        private final int numRoutes;

        private final boolean[][] seen; // performance

        private RouteInterconnects(RouteIndexPairFactory pairFactory, int numRoutes) {
            this.pairFactory = pairFactory;
            this.numRoutes = numRoutes;
            bitSetForIndex = new HashMap<>();
            seen = new boolean[numRoutes][numRoutes];
        }

        int numberOfLinks() {
            return bitSetForIndex.size();
        }

         void addLinksBetween(final short routeIndexA, final short routeIndexB, final SimpleImmutableBitmap links,
                                    final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForRoute,
                                    final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForConnectedRoute) {
            links.getBitIndexes().
                    filter(linkIndex -> overlapBetween(dateOverlapsForRoute, dateOverlapsForConnectedRoute, linkIndex)).
                    map(linkIndex -> getBitSetForPair(routeIndexA, linkIndex)).
                    forEach(bitSet -> bitSet.set(routeIndexB));
        }

        boolean overlapBetween(final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForRoute,
                                       final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForConnectedRoute,
                                       final Short linkIndex) {
            return dateOverlapsForRoute.get(linkIndex) && dateOverlapsForConnectedRoute.get(linkIndex);
        }

        private BitSet getBitSetForPair(short routeIndexA, short linkIndex) {
            int position = getPosition(routeIndexA, linkIndex);
            if (seen[routeIndexA][linkIndex]) {
                return bitSetForIndex.get(position);
            }

            final BitSet bitSet = new BitSet();
            bitSetForIndex.put(position, bitSet);
            seen[routeIndexA][linkIndex] = true;
            return bitSet;

        }

        private int getPositionFor(final RouteIndexPair routeIndexPair) {
            return getPosition(routeIndexPair.first(), routeIndexPair.second());
        }

        private int getPosition(final short indexA, final short indexB) {
            return (indexA * numRoutes) + indexB;
        }

        // re-expand from (A,C) -> B into: (A,B) (B,C)
        public Stream<Pair<RouteIndexPair, RouteIndexPair>> getLinksFor(RouteIndexPair indexPair) {
            final int position = getPositionFor(indexPair);

            final BitSet connectingRoutes = bitSetForIndex.get(position);
            return connectingRoutes.stream().
                    mapToObj(link -> (short) link).
                    map(link -> Pair.of(pairFactory.get(indexPair.first(), link), pairFactory.get(link, indexPair.second())));
        }

        public boolean hasLinksFor(RouteIndexPair indexPair) {
            return seen[indexPair.first()][indexPair.second()];
        }
    }
}
