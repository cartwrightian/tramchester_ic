package com.tramchester.graph.search.routes;

import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.collections.SimpleImmutableBitmap;
import com.tramchester.repository.InterchangeRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Stream;

class RouteInterconnectRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterconnectRepository.class);

    private final RouteIndex routeIndex;
    private final RouteIndexPairFactory pairFactory;
    private final int numRoutes;
    private final InterchangeRepository pairToInterchange;
    private final RouteCostMatrix parent;

    private final List<RouteInterconnects> links;

    public RouteInterconnectRepository(RouteIndexPairFactory pairFactory, int numRoutes, RouteIndex routeIndex, InterchangeRepository pairToInterchange,
                                       RouteCostMatrix parent) {
        this.pairFactory = pairFactory;
        this.numRoutes = numRoutes;
        this.pairToInterchange = pairToInterchange;
        this.parent = parent;
        this.routeIndex = routeIndex;
        links = new ArrayList<>(RouteCostMatrix.MAX_DEPTH);
    }

    public void start() {
        // init only here
        for (int depth = 0; depth < RouteCostMatrix.MAX_DEPTH; depth++) {
            RouteInterconnects routeInterconnects = new RouteInterconnects(pairFactory, numRoutes);
            links.add(routeInterconnects);
        }
    }

    private RouteInterconnects forDepth(final int depth) {
        guardDepth(depth);
        return links.get(depth);
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

        final short degree = parent.getDegree(indexPair);
        final RoutePair missing = routeIndex.getPairFor(indexPair);
        final String message = MessageFormat.format("Missing indexPair {0} ({1}) at depth {2} actual degree for pair {3}",
                indexPair, missing, depth, degree);
        logger.error(message);
        if (!missing.isDateOverlap()) {
            logger.warn("Also no date overlap between " + missing);
        }
        if (!pairToInterchange.hasInterchangeFor(indexPair)) {
            logger.warn("Also no interchange for " + missing);
        }
        throw new RuntimeException(message);
    }

    public RouteInterconnects forDegree(final int currentDegree) {
        return forDepth(currentDegree - 1);
    }

    public Stream<Pair<RouteIndexPair, RouteIndexPair>> forDegree(final int degree, final RouteIndexPair indexPair) {
        return forDepth(degree - 1, indexPair);
    }

    public void clear() {
        links.clear();
    }


    public static class RouteInterconnects {
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

        public int numberOfLinks() {
            return bitSetForIndex.size();
        }

        public void addLinksBetween(final short routeIndexA, final short routeIndexB, final SimpleImmutableBitmap links,
                                    final RouteCostMatrix.RouteOverlaps dateOverlapsForRoute,
                                    final RouteCostMatrix.RouteOverlaps dateOverlapsForConnectedRoute) {
            links.getBitIndexes().
                    filter(linkIndex -> overlapBetween(dateOverlapsForRoute, dateOverlapsForConnectedRoute, linkIndex)).
                    map(linkIndex -> getBitSetForPair(routeIndexA, linkIndex)).
                    forEach(bitSet -> bitSet.set(routeIndexB));
        }

        private boolean overlapBetween(final RouteCostMatrix.RouteOverlaps dateOverlapsForRoute,
                                       final RouteCostMatrix.RouteOverlaps dateOverlapsForConnectedRoute,
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
