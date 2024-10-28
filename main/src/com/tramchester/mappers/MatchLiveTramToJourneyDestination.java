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

    public boolean matchesJourneyDestination(UpcomingDeparture dueTram, final IdSet<Station> origChangeStationsId,
                                             IdFor<Station> finalStationId) {

        // this should no longer happen...
        final IdSet<Station> changeStationIds;
        if (origChangeStationsId.contains(finalStationId)) {
            logger.warn("initial destinations " + origChangeStationsId + " incorrectly contains "
                    + finalStationId + " so removing");
            changeStationIds = IdSet.copy(origChangeStationsId);
            changeStationIds.remove(finalStationId);
        } else {
            changeStationIds = origChangeStationsId;
        }

        final Station dueTramDestination = dueTram.getDestination();
        if (finalStationId.equals(dueTramDestination.getId())) {
            // quick win, tram is going to our final destination
            return true;
        }

        final Station displayLocation = dueTram.getDisplayLocation();
        final TramDate date = TramDate.of(dueTram.getDate());

        // check for trams "towards" our destination
        final Station finalStation = stationRepository.getStationById(finalStationId);
        final IdSet<Route> finalStationDropOffs = finalStation.getDropoffRoutes().stream().collect(IdSet.collector());
        if (anyRouteOverlap(dueTram, finalStationDropOffs)) {
            boolean callsAtDest = stopOrderChecker.check(date, displayLocation, finalStation, dueTramDestination) ||
                    stopOrderChecker.check(date, displayLocation, dueTramDestination, finalStation);
            if (callsAtDest) {
                return true; // else check on change stations
            }
        }

        // match on change stations?
        final Set<Station> changeStations = changeStationIds.stream().
                 map(stationRepository::getStationById).
                collect(Collectors.toSet());

        boolean callsAtChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(dueTram, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, callingStation, dueTramDestination));

        if (callsAtChangeStation) {
            return true;
        }

        boolean towardsChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(dueTram, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, dueTramDestination, callingStation));

        if (towardsChangeStation) {
            return true;
        }

//        final IdSet<Route> dropOffsAtDest = getDropoffsFor(initialStations);
//        if (anyRouteOverlap(dueTram, dropOffsAtDest)) {
//            // now need to check direction, is the due tram destination in the same direction as the journey destination?
//
////            final boolean callsAt = initialStations.stream().
////                    anyMatch(journeyDest -> stopOrderChecker.check(date, dueTram.getDisplayLocation(), journeyDest, dueTramDestination));
//            Set<Station> intermediates = initialStations.stream().
//                    filter(initialStation -> stopOrderChecker.check(date, displayLocation, initialStation, dueTramDestination)).
//                    collect(Collectors.toSet());
//
//            if (!intermediates.isEmpty()) {
//
//                // check if due tram is going in wrong direction
//                boolean correctDir = stopOrderChecker.check(date, displayLocation, finalStation, dueTramDestination) ||
//                        stopOrderChecker.check(date, displayLocation, dueTramDestination, finalStation);
//                if (correctDir) {
//                    return true;
//                }
//            }
//
//            // due tram is going towards our destination, it's on the way
//            return initialStations.stream().
//                    anyMatch(journeyDest -> stopOrderChecker.check(date, displayLocation, dueTramDestination, journeyDest));
//        }

        // todo into debug
        logger.info("Did not match due tram " + dueTram + " with any of " + changeStationIds + " or " + finalStationId);
        return false;

    }

    private boolean anyRouteOverlap(UpcomingDeparture dueTram, Station station) {
        IdSet<Route> dropOffIds = station.getDropoffRoutes().stream().collect(IdSet.collector());
        return anyRouteOverlap(dueTram, dropOffIds);
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
