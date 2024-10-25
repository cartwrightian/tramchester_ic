package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
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

    public boolean matchesJourneyDestination(UpcomingDeparture dueTram, IdSet<Station> originalInitialDestinations,
                                             IdFor<Station> finalStationId) {

        // TODO Make sure finalStationId isn't in initialStationIds
        final IdSet<Station> initialStationIds;
        if (originalInitialDestinations.contains(finalStationId)) {
            logger.warn("initial destinations " + originalInitialDestinations + " incorrectly contains "
                    + finalStationId + " so removing");
            initialStationIds = IdSet.copy(originalInitialDestinations);
            initialStationIds.remove(finalStationId);
        } else {
            initialStationIds = originalInitialDestinations;
        }

        Station dueTramDestination = dueTram.getDestination();
        if (finalStationId.equals(dueTramDestination.getId())) {
            // quick win, tram is going to our final destination
            return true;
        }

        final Station displayLocation = dueTram.getDisplayLocation();
        final TramDate date = TramDate.of(dueTram.getDate());

        final Station finalStation = stationRepository.getStationById(finalStationId);
        final IdSet<Route> finalStationDropOffs = finalStation.getDropoffRoutes().stream().collect(IdSet.collector());
        if (anyRouteOverlap(dueTram, finalStationDropOffs)) {
            boolean callsAtDest = stopOrderChecker.check(date, displayLocation, finalStation, dueTramDestination) ||
                    stopOrderChecker.check(date, displayLocation, dueTramDestination, finalStation);
            if (callsAtDest) {
                return true; // else check on initial destinations
            }
        }

        // match on calling stations?
        final Set<Station> initialStations = initialStationIds.stream().
                 map(stationRepository::getStationById).collect(Collectors.toSet());
        final IdSet<Route> dropOffsAtDest = getDropoffsFor(initialStations);

        if (anyRouteOverlap(dueTram, dropOffsAtDest)) {
            // now need to check direction, is the due tram destination in the same direction as the journey destination?

//            final boolean callsAt = initialStations.stream().
//                    anyMatch(journeyDest -> stopOrderChecker.check(date, dueTram.getDisplayLocation(), journeyDest, dueTramDestination));
            Set<Station> intermediates = initialStations.stream().
                    filter(initialStation -> stopOrderChecker.check(date, displayLocation, initialStation, dueTramDestination)).
                    collect(Collectors.toSet());

            if (!intermediates.isEmpty()) {

                // check if due tram is going in wrong direction
                boolean correctDir = stopOrderChecker.check(date, displayLocation, finalStation, dueTramDestination) ||
                        stopOrderChecker.check(date, displayLocation, dueTramDestination, finalStation);
                if (correctDir) {
                    return true;
                }
            }

            // due tram is going towards our destination, it's on the way
            return initialStations.stream().
                    anyMatch(journeyDest -> stopOrderChecker.check(date, displayLocation, dueTramDestination, journeyDest));
        }
        // todo into debug
        logger.info("Did not match due tram " + dueTram + " with any of " + initialStationIds + " or " + finalStationId);
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
