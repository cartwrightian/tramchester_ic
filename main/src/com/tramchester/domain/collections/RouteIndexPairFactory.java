package com.tramchester.domain.collections;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.repository.NumberOfRoutes;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@LazySingleton
public class RouteIndexPairFactory {
    private final short numberOfRoutes;
    private final Map<Integer, RouteIndexPair> cache;

    @Inject
    public RouteIndexPairFactory(NumberOfRoutes repository) {
        int numRoutes = repository.numberOfRoutes();
        if (numRoutes > Short.MAX_VALUE) {
            throw new RuntimeException("Too many routes " + numRoutes);
        }
        numberOfRoutes = (short) numRoutes;
        cache = new HashMap<>();
    }

    public RouteIndexPair get(final short a, final short b) {
        if (a >= numberOfRoutes) {
            throw new RuntimeException("First argument " + a + " is out of range " + numberOfRoutes);
        }
        if (b >= numberOfRoutes) {
            throw new RuntimeException("Second argument " + b + " is out of range " + numberOfRoutes);
        }

        final int rank = getRank(a, b);
        if (cache.containsKey(rank)) {
            return cache.get(rank);
        }
        final RouteIndexPair pair = RouteIndexPair.of(a, b);
        cache.put(rank, pair);
        return pair;
    }

    private int getRank(final short a, final short b) {
        return (numberOfRoutes * a) + b;
    }

}
