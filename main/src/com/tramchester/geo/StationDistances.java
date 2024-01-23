package com.tramchester.geo;

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

    public FindDistancesTo findDistancesTo(LocationSet destinations) {
        return new FindDistancesTo(destinations);
    }

    public class FindDistancesTo {
        private final Set<GridPosition> grids;

        public FindDistancesTo(LocationSet locations) {
            grids = locations.stream().map(Location::getGridPosition).filter(GridPosition::isValid).collect(Collectors.toSet());
        }

        public long toStation(final IdFor<Station> stationId) {
            final Station station = stationRepository.getStationById(stationId);
            final GridPosition gridPosition = station.getGridPosition();
            final Optional<Long> find = grids.stream().map(grid -> GridPositions.distanceTo(gridPosition, grid)).min(Long::compare);
            return find.orElse(Long.MAX_VALUE);
        }
    }


}
