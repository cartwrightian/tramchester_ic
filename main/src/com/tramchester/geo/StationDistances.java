package com.tramchester.geo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class StationDistances {
    private final StationRepository stationRepository;

    @Inject
    public StationDistances(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public FindDistancesTo findDistancesTo(final LocationSet destinations) {
        return new FindDistancesTo(destinations);
    }

    public class FindDistancesTo {
        private final Set<GridPosition> destinationGrids;

        // only look up distance to destination once for any station
        // note: assumes FindDistancesTo lifetime is limited to one query, otherwise need tuning of cache creation to
        // avoid memory issues
        private final Cache<IdFor<Station>, Long> cache;

        public FindDistancesTo(final LocationSet destinationLocations) {
            destinationGrids = destinationLocations.stream().map(Location::getGridPosition).filter(GridPosition::isValid).collect(Collectors.toSet());
            cache = Caffeine.newBuilder().build();
        }

        public long toStation(final IdFor<Station> stationId) {
            return cache.get(stationId, this::getMinDistance);
        }

        private Long getMinDistance(final IdFor<Station> stationId) {
            final Station station = stationRepository.getStationById(stationId);
            final GridPosition gridPosition = station.getGridPosition();
            final Optional<Long> find = destinationGrids.stream().map(grid -> GridPositions.distanceTo(gridPosition, grid)).min(Long::compare);
            return find.orElse(Long.MAX_VALUE);
        }

        public int compare(final IdFor<Station> stationIdA, final IdFor<Station> stationIdB) {
            final long distanceA = toStation(stationIdA);
            final long distanceB = toStation(stationIdB);

            return Long.compare(distanceA, distanceB);
        }
    }


}
