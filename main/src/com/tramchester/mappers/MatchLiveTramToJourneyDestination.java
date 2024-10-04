package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class MatchLiveTramToJourneyDestination {
    private static final Logger logger = LoggerFactory.getLogger(MatchLiveTramToJourneyDestination.class);

    private final StationRepository stationRepository;
    private final StopOrderChecker stopOrderChecker;

    @Inject
    public MatchLiveTramToJourneyDestination(StationRepository stationRepository, StopOrderChecker stopOrderChecker) {
        this.stationRepository = stationRepository;
        this.stopOrderChecker = stopOrderChecker;
    }

    public boolean matchesJourneyDestination(UpcomingDeparture dueTram, IdSet<Station> journeyDestinationsIds) {
        Station dueTramDestination = dueTram.getDestination();
        if (journeyDestinationsIds.contains(dueTramDestination.getId())) {
            // quick win, tram is going to the journey destination
            return true;
        }
        final Set<Station> journeyDestinations = journeyDestinationsIds.stream().map(stationRepository::getStationById).collect(Collectors.toSet());
        final IdSet<Route> dropOffsAtDest = getDropoffsFor(journeyDestinations);

        if (anyRouteOverlap(dueTram, dropOffsAtDest)) {
            // now need to check direction, is the due tram destination in the same direction as the journey destination?
            final TramDate date = TramDate.of(dueTram.getDate());

            boolean callsAt = journeyDestinations.stream().
                    anyMatch(journeyDest -> stopOrderChecker.check(date, dueTram.getDisplayLocation(), journeyDest, dueTramDestination));

            if (callsAt) {
                return true;
            }

            // due tram is going towards our destination, it's on the way
            return journeyDestinations.stream().
                    anyMatch(journeyDest -> stopOrderChecker.check(date, dueTram.getDisplayLocation(), dueTramDestination, journeyDest));
        }
        // todo into debug
        logger.info("Did not match due tram " + dueTram + " with any of " + journeyDestinations);
        return false;

    }

    private IdSet<Route> getDropoffsFor(final Set<Station> stations) {
        return stations.stream().
                flatMap(station -> station.getDropoffRoutes().stream()).
                collect(IdSet.collector());
    }

    private boolean anyRouteOverlap(UpcomingDeparture departure, IdSet<Route> dropOffsAtDest) {
        Station dueTramDest = departure.getDestination();
        IdSet<Route> dropOffsForDueTram = dueTramDest.getDropoffRoutes().stream().collect(IdSet.collector());
        return !IdSet.intersection(dropOffsAtDest, dropOffsForDueTram).isEmpty();
    }
}
