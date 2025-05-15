package com.tramchester.domain.collections;

import com.tramchester.domain.RoutePair;
import com.tramchester.graph.search.routes.RouteIndex;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

public class PairOfRouteIndexPair {
    private final RouteIndexPair pairA;
    private final RouteIndexPair pairB;

    public PairOfRouteIndexPair(RouteIndexPair pairA, RouteIndexPair pairB) {
        this.pairA = pairA;
        this.pairB = pairB;
    }

    public static PairOfRouteIndexPair of(final RouteIndexPair pairA, final RouteIndexPair pairB) {
        if (pairA==null) {
            throw new RuntimeException("pairA was null");
        }
        if (pairB==null) {
            throw new RuntimeException("pairB was null");
        }
        return new PairOfRouteIndexPair(pairA, pairB);
    }

    public Pair<RoutePair, RoutePair> resolve(final RouteIndex routeIndex) {
        return Pair.of(routeIndex.getPairFor(pairA), routeIndex.getPairFor(pairB));
    }

    public RouteIndexPair getLeft() {
        return pairA;
    }

    public RouteIndexPair getRight() {
        return pairB;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PairOfRouteIndexPair that)) return false;
        return Objects.equals(pairA, that.pairA) && Objects.equals(pairB, that.pairB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pairA, pairB);
    }

    @Override
    public String toString() {
        return "PairOfRouteIndexPair{" +
                "pairA=" + pairA +
                ", pairB=" + pairB +
                '}';
    }
}
