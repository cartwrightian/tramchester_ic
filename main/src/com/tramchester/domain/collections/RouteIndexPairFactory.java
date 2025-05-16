package com.tramchester.domain.collections;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.repository.NumberOfRoutes;
import jakarta.inject.Inject;

@LazySingleton
public class RouteIndexPairFactory {
    private final short numberOfRoutes;
    private final RouteIndexPair[][] cache;

    @Inject
    public RouteIndexPairFactory(final NumberOfRoutes repository) {
        final int numRoutes = repository.numberOfRoutes();
        if (numRoutes > Short.MAX_VALUE) {
            throw new RuntimeException("Too many routes " + numRoutes);
        }
        numberOfRoutes = (short) numRoutes;
        cache = new RouteIndexPair[numRoutes][numRoutes];
        for (short i=0; i<numberOfRoutes; i++) {
            for(short j=0; j<numberOfRoutes; j++) {
                cache[i][j] = RouteIndexPair.of(i,j);
            }
        }
    }

    public RouteIndexPair get(final short a, final short b) {
        if (a >= numberOfRoutes) {
            throw new RuntimeException("First argument " + a + " is out of range " + numberOfRoutes);
        }
        if (b >= numberOfRoutes) {
            throw new RuntimeException("Second argument " + b + " is out of range " + numberOfRoutes);
        }

        return cache[a][b];
    }

}
