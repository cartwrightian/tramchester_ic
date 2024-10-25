package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.mappers.MatchLiveTramToJourneyDestination;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class DeparturesMapper {
    public static final String DUE = "Due";

    private final MatchLiveTramToJourneyDestination matchLiveTramToJourneyDestination;

    @Inject
    public DeparturesMapper(MatchLiveTramToJourneyDestination matchLiveTramToJourneyDestination) {
        this.matchLiveTramToJourneyDestination = matchLiveTramToJourneyDestination;
    }

    public Set<DepartureDTO> mapToDTO(Collection<UpcomingDeparture> dueTrams, LocalDateTime lastUpdate, IdSet<Station> journeyDestinations,
                                      IdFor<Station> finalStation) {
        return dueTrams.stream().
                    map(dueTram -> getDepartureDTO(lastUpdate, dueTram, journeyDestinations, finalStation))
                    .collect(Collectors.toSet());
    }

    private DepartureDTO getDepartureDTO(LocalDateTime lastUpdate, UpcomingDeparture dueTram, IdSet<Station> initialJourneyDestinations,
                                      IdFor<Station> finalStation) {
        boolean matchesJourney = matchLiveTramToJourneyDestination.matchesJourneyDestination(dueTram, initialJourneyDestinations, finalStation);
        return new DepartureDTO(dueTram.getDisplayLocation(), dueTram, lastUpdate, matchesJourney);
    }

    public Set<DepartureDTO> mapToDTO(Collection<UpcomingDeparture> trams, LocalDateTime lastUpdate) {
        return trams.stream().
                map(dueTram -> new DepartureDTO(dueTram.getDisplayLocation(), dueTram, lastUpdate, false))
                .collect(Collectors.toSet());
    }
}
