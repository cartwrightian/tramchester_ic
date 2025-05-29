package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.ComponentThatCaches;
import com.tramchester.caching.DataCache;
import com.tramchester.caching.FileDataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.dataimport.data.RoutePairInterconnectsData;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RouteInterconnectRepository extends ComponentThatCaches<RoutePairInterconnectsData, RouteInterconnectRepository.RouteInterconnects> {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterconnectRepository.class);

    private final RouteIndex routeIndex;
    private final RouteDateAndDayOverlap routeDateAndDayOverlap;
    private final RouteIndexPairFactory pairFactory;
    private final int numRoutes;
    private final InterchangeRepository interchangeRepository;
    private final RouteCostMatrix routeCostMatrix;

    private final RouteInterconnects interconnectsForDepth;
    private final GraphFilterActive graphFilter;
    private final boolean warnForMissing;

    @Inject
    public RouteInterconnectRepository(RouteIndexPairFactory pairFactory, NumberOfRoutes numberOfRoutes, RouteIndex routeIndex,
                                       InterchangeRepository interchangeRepository, RouteCostMatrix routeCostMatrix,
                                       RouteDateAndDayOverlap routeDateAndDayOverlap, DataCache dataCache, GraphFilterActive graphFilter,
                                       TramchesterConfig config) {
        super(dataCache, RoutePairInterconnectsData.class);
        this.pairFactory = pairFactory;
        this.numRoutes = numberOfRoutes.numberOfRoutes();
        this.interchangeRepository = interchangeRepository;
        this.routeCostMatrix = routeCostMatrix;
        this.routeIndex = routeIndex;
        this.routeDateAndDayOverlap = routeDateAndDayOverlap;
        interconnectsForDepth = new RouteInterconnects(RouteCostMatrix.MAX_DEPTH);
        this.graphFilter = graphFilter;

        if (graphFilter.isActive()) {
            warnForMissing = false;
        } else {
            // for mixed modes don't warn on lack on route overlap
            this.warnForMissing = config.getTransportModes().size()==1;
        }
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        interconnectsForDepth.start(pairFactory, numRoutes);
        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            createInterconnects(routeDateAndDayOverlap);
        }
        else {
            if (cachePresent(interconnectsForDepth)) {
                super.loadFromCache(interconnectsForDepth);
            } else {
                createInterconnects(routeDateAndDayOverlap);
            }
        }
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        if (!graphFilter.isActive()) {
            super.saveCacheIfNeeded(interconnectsForDepth);
        }
        interconnectsForDepth.clear();
        logger.info("stopped");
    }

    private void createInterconnects(final RouteDateAndDayOverlap routeDateAndDayOverlap) {
        // degree 1 = depth 0 = interchanges directly
        for (int currentDegree = 1; currentDegree <= RouteCostMatrix.MAX_DEPTH; currentDegree++) {
            createInterconnects(routeDateAndDayOverlap, currentDegree);
        }
    }

    private void createInterconnects(final RouteDateAndDayOverlap routeDateAndDayOverlap, final int currentDegree) {
        final int totalPossible = numRoutes * numRoutes;
        if (currentDegree<1) {
            throw new RuntimeException("Only call for >1 , got " + currentDegree);
        }

        final ImmutableIndexedBitSet matrixForDegree = routeCostMatrix.getDegree(currentDegree);

        logger.info("Create route pair connection map for degree " + currentDegree + " matrixForDegree bits set " + matrixForDegree.numberOfBitsSet());

        if (matrixForDegree.numberOfBitsSet()>0) {
            // zero indexed
            final RoutePairInterconnects routePairInterconnects = forDegree(currentDegree);

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
                    routePairInterconnects.addLinksBetween(currentRouteIndex, connectedRoute, intermediates,
                            dateOverlapsForRoute, dateOverlapsForConnectedRoute);
                });
            }

            final long took = Duration.between(startTime, Instant.now()).toMillis();
            final int added = routePairInterconnects.numberOfLinks();
            final double percentage = ((double)added)/((double)totalPossible) * 100D;
            logger.info(String.format("Added route interconnection pairs %s (%s %%) Degree %s in %s ms",
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

    private Stream<PairOfRouteIndexPair> forDepth(final int depth, final RouteIndexPair indexPair) {
        guardDepth(depth);
        final RoutePairInterconnects routePairInterconnects = interconnectsForDepth.forDepth(depth);
        if (routePairInterconnects.hasLinksFor(indexPair)) {
            return routePairInterconnects.getLinksFor(indexPair);
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

    private RoutePairInterconnects forDegree(final int currentDegree) {
        return forDepth(currentDegree - 1);
    }

    private RoutePairInterconnects forDepth(final int depth) {
        guardDepth(depth);
        return interconnectsForDepth.forDepth(depth);
    }

    private Stream<PairOfRouteIndexPair> forDegree(final int degree, final RouteIndexPair indexPair) {
        return forDepth(degree - 1, indexPair);
    }

    // test support
    public Set<Pair<RoutePair, RoutePair>> getBackTracksFor(int degree, RouteIndexPair indexPair) {
        return forDegree(degree, indexPair).
                //map(pair -> Pair.of(routeIndex.getPairFor(pair.getLeft()), routeIndex.getPairFor(pair.getRight()))).
                map(pair -> pair.resolve(routeIndex)).
                collect(Collectors.toSet());
    }

    // test support
    public int getNumberBacktrackFor(final int depth) {
        return forDegree(depth).numberOfLinks();
    }

    /***
     * Finds the paths (between the routes, via interchanges) between the routes given in the indexPair, for the given
     * date overlaps between the 2, filtering the interchanges using the provided filter.
     * @param indexPair the routes to find path between
     * @param dateOverlaps representation of the dates the two routes overlap (aka run) on
     * @param interchangeFilter an (inclusive) filter for the interchanges to use when finding the path
     * @return paths between the 2 routes from the indexPair
     */
    public PathResults getInterchangesFor(final RouteIndexPair indexPair, final ImmutableIndexedBitSet dateOverlaps,
                                          final Function<InterchangeStation, Boolean> interchangeFilter) {
        final int degree = routeCostMatrix.getDegree(indexPair);

        if (degree==Byte.MAX_VALUE) {
            if (warnForMissing) {
                logger.warn("No degree found for " + routeIndex.getPairFor(indexPair));
            }
            return new PathResults.NoPathResults();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Get interchanges for %s with initial degree %s", HasId.asIds(routeIndex.getPairFor(indexPair)), degree));
        }

        final ImmutableIndexedBitSet changesForDegree = routeCostMatrix.
                getDegree(degree).
                getCopyOfRowAndColumn(indexPair.first(), indexPair.second());
        // apply mask to filter out unavailable dates/modes quickly
        final IndexedBitSet withDateApplied = IndexedBitSet.and(changesForDegree, dateOverlaps);

        if (withDateApplied.isSet(indexPair)) {
            final QueryPathsWithDepth.QueryPath pathFor = getPathFor(indexPair, degree, interchangeFilter);
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
            // did we find interchange?
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
            final Set<InterchangeStation> changes = getFilteredInterchangesFor(indexPair, interchangeFilter);

            if (changes.isEmpty()) {
                // might not be available due to interchange filter
                return QueryPathsWithDepth.ZeroPaths.get();
            } else {
                return QueryPathsWithDepth.AnyOfInterchanges.Of(changes);
            }
        } else {
            final int lowerDegree = currentDegree-1;

            final Stream<PairOfRouteIndexPair> underlying = forDegree(lowerDegree, indexPair);

            final Set<QueryPathsWithDepth.BothOf> combined = underlying.
                    map(pair -> expandPathFor(pair, lowerDegree, interchangeFilter)).
                    filter(QueryPathsWithDepth.BothOf::hasAny).
                    collect(Collectors.toSet());

            return new QueryPathsWithDepth.AnyOf(combined);
        }
    }

    private @NotNull Set<InterchangeStation> getFilteredInterchangesFor(final RouteIndexPair indexPair,
                                                                        final Function<InterchangeStation, Boolean> interchangeFilter) {
        return interchangeRepository.getInterchangesFor(indexPair).
                filter(interchangeFilter::apply).
                collect(Collectors.toSet());
    }

    @NotNull
    private QueryPathsWithDepth.BothOf expandPathFor(final PairOfRouteIndexPair pair, final int degree,
                                                     final Function<InterchangeStation, Boolean> interchangeFilter) {
        final QueryPathsWithDepth.QueryPath pathA = getPathFor(pair.getLeft(), degree, interchangeFilter);
        final QueryPathsWithDepth.QueryPath pathB = getPathFor(pair.getRight(), degree, interchangeFilter);
        return new QueryPathsWithDepth.BothOf(pathA, pathB);
    }

    public static class RouteInterconnects implements FileDataCache.CachesData<RoutePairInterconnectsData> {
        private final List<RoutePairInterconnects> interconnectsForDepth;
        private final int maxDepth;

        public RouteInterconnects(int maxDepth) {
            this.maxDepth = maxDepth;
            interconnectsForDepth = new ArrayList<>();

        }

        public void start(final RouteIndexPairFactory pairFactory, final int numRoutes) {
            for (int depth = 0; depth < maxDepth; depth++) {
                final RoutePairInterconnects routePairInterconnects = new RoutePairInterconnects(pairFactory, numRoutes);
                interconnectsForDepth.add(routePairInterconnects);
            }
        }

        public void clear() {
            interconnectsForDepth.clear();
        }

        private RoutePairInterconnects forDepth(final int depth) {
            return interconnectsForDepth.get(depth);
        }

        @Override
        public void cacheTo(HasDataSaver<RoutePairInterconnectsData> hasDataSaver) {
            try (HasDataSaver.ClosableDataSaver<RoutePairInterconnectsData> saver = hasDataSaver.get()) {
                for (int depth = 0; depth < maxDepth; depth++) {
                    RoutePairInterconnects current = interconnectsForDepth.get(depth);
                    current.cacheTo(depth, saver);
                }
            } catch (Exception e) {
                logger.error("Exception while writing cache",e);
            }
        }

        @Override
        public String getFilename() {
            return "route_interconnects.json";
        }

        @Override
        public void loadFrom(Stream<RoutePairInterconnectsData> stream) throws FileDataCache.CacheLoadException {
            try {
                stream.forEach(item -> interconnectsForDepth.get(item.getDepth()).insert(item));
            }
            catch (Exception e) {
                String msg = "Load from cache failed";
                logger.error(msg, e);
                throw new FileDataCache.CacheLoadException(msg);
            }
        }


    }

    private static class RoutePairInterconnects {
        private final RouteIndexPairFactory pairFactory;

        // (A, C) -> (A, B) (B ,C)
        // reduces to => (A,C) -> B

        // pair to connecting route index (A,B) -> [C], compute the Integer as position in bitset from A&B
        private final Map<Integer, BitSet> bitSetForIndex;
        private final int numRoutes;

        private final boolean[][] haveBitset; // performance

        private RoutePairInterconnects(RouteIndexPairFactory pairFactory, int numRoutes) {
            this.pairFactory = pairFactory;
            this.numRoutes = numRoutes;
            bitSetForIndex = new HashMap<>();
            haveBitset = new boolean[numRoutes][numRoutes];
        }

        int numberOfLinks() {
            return bitSetForIndex.size();
        }

        void addLinksBetween(final short routeIndexA, final short routeIndexB, final SimpleImmutableBitmap links,
                                    final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForRoute,
                                    final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForConnectedRoute) {
            links.getBitIndexes().
                    filter(linkIndex -> overlapBetween(dateOverlapsForRoute, dateOverlapsForConnectedRoute, linkIndex)).
                    map(linkIndex -> createBitSetForPair(routeIndexA, linkIndex)).
                    forEach(bitSet -> bitSet.set(routeIndexB));
        }

        private boolean overlapBetween(final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForRoute,
                                       final RouteDateAndDayOverlap.RouteOverlaps dateOverlapsForConnectedRoute,
                                       final Short linkIndex) {
            return dateOverlapsForRoute.get(linkIndex) && dateOverlapsForConnectedRoute.get(linkIndex);
        }

        private BitSet createBitSetForPair(final short routeA, final short routeB) {
            final int position = getPosition(routeA, routeB);
            if (haveBitset[routeA][routeB]) {
                return bitSetForIndex.get(position);
            }

            final BitSet bitSet = new BitSet();
            bitSetForIndex.put(position, bitSet);
            haveBitset[routeA][routeB] = true;
            return bitSet;
        }

        private int getPositionFor(final RouteIndexPair routeIndexPair) {
            return getPosition(routeIndexPair.first(), routeIndexPair.second());
        }

        private int getPosition(final short indexA, final short indexB) {
            return (indexA * numRoutes) + indexB;
        }

        public boolean hasLinksFor(final RouteIndexPair indexPair) {
            return haveBitset[indexPair.first()][indexPair.second()];
        }

        // re-expand from (A,C) -> B into: (A,B) (B,C)
        public Stream<PairOfRouteIndexPair> getLinksFor(final RouteIndexPair indexPair) {
            final int position = getPositionFor(indexPair);

            final BitSet connectingRoutes = bitSetForIndex.get(position);
            return connectingRoutes.stream().
                    mapToObj(link -> indexPair.expandWith(pairFactory, link));
//                    mapToObj(link -> (short) link).
//                    map(link -> PairOfRouteIndexPair.of(pairFactory.get(indexPair.first(), link),
//                            pairFactory.get(link, indexPair.second())));
        }

        public void cacheTo(final int depth, final HasDataSaver.ClosableDataSaver<RoutePairInterconnectsData> saver) {
            bitSetForIndex.entrySet().stream().
                    filter(entry -> !entry.getValue().isEmpty()).
                    map(entry -> createCacheItem(depth, entry.getKey(), entry.getValue())).
                    forEach(saver::write);
        }

        private RoutePairInterconnectsData createCacheItem(final int depth, final int position, final BitSet bitSet) {
            short size = (short) numRoutes;
            short firstIndex = (short) (position / size);
            short secondIndex = (short) (position % size);
            // todo remove?
            if (getPosition(firstIndex, secondIndex)!=position) {
                throw new RuntimeException("Position mismatch, expected " + position + " but got " + getPosition(firstIndex, secondIndex));
            }
            return new RoutePairInterconnectsData(depth, firstIndex, secondIndex, bitSet);
        }

        public void insert(final RoutePairInterconnectsData item) {
            final short routeA = item.getRouteA();
            final short routeB = item.getRouteB();
            final int position = getPosition(routeA, routeB);
            final BitSet overlaps = item.getOverlaps();
            haveBitset[routeA][routeB] = true;
            bitSetForIndex.put(position, overlaps);
        }
    }
}
