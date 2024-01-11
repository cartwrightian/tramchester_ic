package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.collections.SimpleBitmap;
import com.tramchester.domain.collections.SimpleImmutableBitmap;
import com.tramchester.repository.NumberOfRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.IntStream;

@LazySingleton
public class RouteDateAndDayOverlap {
    private static final Logger logger = LoggerFactory.getLogger(RouteDateAndDayOverlap.class);

    // Create a bitmask corresponding to the dates and days routes overlap
    // NOTE: this is for route overlaps only

    private final SimpleBitmap[] overlapMasks;
    private final int numberOfRoutes;
    private final RouteIndex index;
    private int numberSet = -1;

    @Inject
    public RouteDateAndDayOverlap(RouteIndex index, NumberOfRoutes numberOfRoutes) {
        this.index = index;
        this.numberOfRoutes = numberOfRoutes.numberOfRoutes();
        overlapMasks = new SimpleBitmap[this.numberOfRoutes];
    }

    @PostConstruct
    public void start() {
        logger.info("Creating matrix for route date/day overlap");
        numberSet = 0;

        for (short i = 0; i < numberOfRoutes; i++) {
            final Route from = index.getRouteFor(i);
            final SimpleBitmap resultsForRoute = SimpleBitmap.create(numberOfRoutes);
            final int fromIndex = i;
            // thread safety: split into list and then application of list to bitset
            final List<Integer> toSet = IntStream.range(0, numberOfRoutes).
                    //parallel().
                            filter(toIndex -> (fromIndex == toIndex) || from.isDateOverlap(index.getRouteFor((short) toIndex))).
                    boxed().toList();
            numberSet = numberSet + toSet.size();
            toSet.forEach(resultsForRoute::set);
            overlapMasks[i] = resultsForRoute;
        }
        logger.info("Finished matrix for route date/day overlap, added " + numberSet + " overlaps");
    }

    @PreDestroy
    public void clear() {
        for (SimpleBitmap overlapMask : overlapMasks) {
            overlapMask.clear();
        }
    }

    public RouteOverlaps overlapsFor(final short routeIndex) {
        if (numberSet < 0) {
            throw new RuntimeException("populate first");
        }
        return new RouteOverlaps(routeIndex, overlapMasks[routeIndex]);
    }

    public int numberBitsSet() {
        return numberSet;
    }

    public static class RouteOverlaps {
        final short routeIndex;
        final SimpleImmutableBitmap overlaps;

        private RouteOverlaps(short routeIndex, SimpleImmutableBitmap overlaps) {
            this.routeIndex = routeIndex;
            this.overlaps = overlaps;
        }

        public boolean get(final short otherRouteIndex) {
            return overlaps.get(otherRouteIndex);
        }

        public SimpleImmutableBitmap getBitSet() {
            return overlaps;
        }
    }
}
