package com.tramchester.geo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.repository.LocationRepository;
import jakarta.inject.Inject;

import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class LocationDistances {
    private final LocationRepository locationRepository;

    @Inject
    public LocationDistances(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public FindDistancesTo findDistancesTo(final LocationCollection destinations) {
        return new FindDistancesTo(destinations);
    }

    public class FindDistancesTo {
        private final Set<GridPosition> destinationGrids;

        // only look up distance to destination once for any station
        // note: assumes FindDistancesTo lifetime is limited to one query, otherwise need tuning of cache creation to
        // avoid memory issues
        private final Cache<IdFor<? extends Location<?>>, Long> cache;

        public FindDistancesTo(final LocationCollection destinationLocations) {
            destinationGrids = destinationLocations.locationStream().
                    map(Location::getGridPosition).
                    filter(GridPosition::isValid).
                    collect(Collectors.toSet());
            cache = Caffeine.newBuilder().build();
        }

        public long shortestDistanceToDest(final IdFor<? extends Location<?>>stationId) {
            return cache.get(stationId, this::getMinDistance);
        }

        private Long getMinDistance(final IdFor<? extends Location<?>> locationId) {
            final Location<?> location = locationRepository.getLocation(locationId);
            final GridPosition gridPosition = location.getGridPosition();

            final OptionalLong find = destinationGrids.stream().
                    mapToLong(dest -> GridPositions.distanceTo(gridPosition, dest)).
                    min();

//            final Optional<Long> find = destinationGrids.stream().
//                    map(grid -> GridPositions.distanceTo(gridPosition, grid)).
//                    min(Long::compare);

            return find.orElse(Long.MAX_VALUE);
        }

        public int compare(final IdFor<? extends Location<?>> locationA, final IdFor<? extends Location<?>> locationB) {
            final long distanceA = shortestDistanceToDest(locationA);
            final long distanceB = shortestDistanceToDest(locationB);

            return Long.compare(distanceA, distanceB);
        }
    }

}
