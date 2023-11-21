package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.StationRepository;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class RecentJourneysToStations {
    private final StationRepository stationRepository;

    @Inject
    public RecentJourneysToStations(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public Set<Station> from(RecentJourneys recentJourneys, EnumSet<TransportMode> modes) {

        return recentJourneys.stream().map(Timestamped::getId).
                map(Station::createId).
                filter(stationRepository::hasStationId).
                map(stationRepository::getStationById).
                filter(station -> TransportMode.intersects(modes, station.getTransportModes())).
                collect(Collectors.toSet());
    }
}
