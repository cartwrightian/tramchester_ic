package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.mappers.MatchLiveTramToJourneyDestination;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class DeparturesMapper {
    public static final String DUE = "Due";

    private final MatchLiveTramToJourneyDestination matchLiveTramToJourneyDestination;
    private final MapJourneyDTOToStations mapJourneyDTOToStations;

    @Inject
    public DeparturesMapper(MatchLiveTramToJourneyDestination matchLiveTramToJourneyDestination, MapJourneyDTOToStations mapJourneyDTOToStations) {
        this.matchLiveTramToJourneyDestination = matchLiveTramToJourneyDestination;
        this.mapJourneyDTOToStations = mapJourneyDTOToStations;
    }

    public Set<DepartureDTO> mapToDTO(Collection<UpcomingDeparture> dueTrams, LocalDateTime lastUpdate, List<JourneyDTO> journeys) {
        final IdSet<Station> changeStations = mapJourneyDTOToStations.getAllChangeStations(journeys);
        final IdFor<Station> finalStation = mapJourneyDTOToStations.getFinalStationId(journeys);
        return mapToDTO(dueTrams, lastUpdate, changeStations, finalStation);
    }

    private Set<DepartureDTO> mapToDTO(Collection<UpcomingDeparture> dueTrams, final LocalDateTime lastUpdate, final IdSet<Station> changeStations,
                                      final IdFor<Station> finalStation) {
        return dueTrams.stream().
                    map(dueTram -> getDepartureDTO(lastUpdate, dueTram, changeStations, finalStation))
                    .collect(Collectors.toSet());
    }

    private DepartureDTO getDepartureDTO(LocalDateTime lastUpdate, UpcomingDeparture dueTram, IdSet<Station> changeStations,
                                      IdFor<Station> finalStation) {
        boolean matchesJourney = matchLiveTramToJourneyDestination.matchesJourneyDestination(dueTram, changeStations, finalStation);
        return new DepartureDTO(dueTram.getDisplayLocation(), dueTram, lastUpdate, matchesJourney);
    }

    public Set<DepartureDTO> mapToDTO(Collection<UpcomingDeparture> trams, LocalDateTime lastUpdate) {
        return trams.stream().
                map(dueTram -> new DepartureDTO(dueTram.getDisplayLocation(), dueTram, lastUpdate, false))
                .collect(Collectors.toSet());
    }


}
