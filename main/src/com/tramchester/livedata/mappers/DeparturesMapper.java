package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.mappers.MatchDeparturesToJourneyDestination;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class DeparturesMapper {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesMapper.class);

    // TODO Tram Specific
    public static final String DUE = "Due";

    private final MatchDeparturesToJourneyDestination matchLiveTramToJourneyDestination;

    @Inject
    public DeparturesMapper(MatchDeparturesToJourneyDestination matchLiveTramToJourneyDestination) {
        this.matchLiveTramToJourneyDestination = matchLiveTramToJourneyDestination;
    }

    public SortedSet<DepartureDTO> createDepDTOForJourneys(final Collection<UpcomingDeparture> departures, final LocalDateTime lastUpdate,
                                                           final DestinationAndCallingPoints destinationAndCallingPoints) {
        logger.info(format("Checking departures matching %s", destinationAndCallingPoints));

        return departures.stream().
                map(departure -> createDepartureDTO(lastUpdate, departure, destinationAndCallingPoints)).
                collect(Collectors.toCollection(TreeSet::new));
    }

    private DepartureDTO createDepartureDTO(final LocalDateTime lastUpdate, final UpcomingDeparture departure,
                                            final DestinationAndCallingPoints destinationAndCallingPoints) {
        final boolean matchesJourney = matchLiveTramToJourneyDestination.matchesJourneyDestination(departure, destinationAndCallingPoints);

        return new DepartureDTO(departure.getDisplayLocation(), departure, lastUpdate, matchesJourney);
    }

    public SortedSet<DepartureDTO> mapToDTO(final Collection<UpcomingDeparture> departures, final LocalDateTime lastUpdate) {
        return departures.stream().
                map(departure -> new DepartureDTO(departure.getDisplayLocation(), departure, lastUpdate, false))
                .collect(Collectors.toCollection(TreeSet::new));
    }


}
