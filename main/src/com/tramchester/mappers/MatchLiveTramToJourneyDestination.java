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

    public boolean matchesJourneyDestination(final UpcomingDeparture upcomingDeparture, final IdSet<Station> origChangeStationsId,
                                             final IdFor<Station> finalStationId) {

        // this should no longer happen...
        final IdSet<Station> changeStationIds;
        if (origChangeStationsId.contains(finalStationId)) {
            logger.warn("original destinations " + origChangeStationsId + " incorrectly contains "
                    + finalStationId + " so removing");
            changeStationIds = IdSet.copy(origChangeStationsId);
            changeStationIds.remove(finalStationId);
        } else {
            changeStationIds = origChangeStationsId;
        }

        IdFor<Station> dueDestinationId = upcomingDeparture.getDestinationId();

        // TODO Check if dueDestinationId is in station repository, could be an out of bounds station

        if (finalStationId.equals(dueDestinationId)) {
            // quick win, tram is going to our final destination
            return true;
        }

        final Station displayLocation = upcomingDeparture.getDisplayLocation();
        final TramDate date = TramDate.of(upcomingDeparture.getDate());

        // check for trams/trains "towards" our destination
        final Station finalStation = stationRepository.getStationById(finalStationId);
        final IdSet<Route> finalStationDropOffs = finalStation.getDropoffRoutes().stream().collect(IdSet.collector());

        //final Station dueDestination = upcomingDeparture.getDestination();


        if (anyRouteOverlap(upcomingDeparture, finalStationDropOffs)) {
            boolean callsAtDest = stopOrderChecker.check(date, displayLocation, finalStation.getId(), dueDestinationId) ||
                    stopOrderChecker.check(date, displayLocation, dueDestinationId, finalStation.getId());
            if (callsAtDest) {
                return true; // else check on change stations
            }
        }

        // match on change stations?
        final Set<Station> changeStations = changeStationIds.stream().
                 map(stationRepository::getStationById).
                collect(Collectors.toSet());

        boolean callsAtChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(upcomingDeparture, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, callingStation.getId(), dueDestinationId));

        if (callsAtChangeStation) {
            return true;
        }

        boolean towardsChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(upcomingDeparture, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, dueDestinationId, callingStation.getId()));

        if (towardsChangeStation) {
            return true;
        }

        // todo into debug
        logger.info("Did not match due tram " + upcomingDeparture + " with any of " + changeStationIds + " or " + finalStationId);
        return false;

    }

    private boolean anyRouteOverlap(final UpcomingDeparture dueTram, final Station station) {

        // TODO route most matrix

        final IdSet<Route> dropOffIds = station.getDropoffRoutes().stream().collect(IdSet.collector());
        return anyRouteOverlap(dueTram, dropOffIds);
    }

    private boolean anyRouteOverlap(final UpcomingDeparture departure, final IdSet<Route> dropOffsAtDest) {

        // TODO route most matrix

        final Station departureDestination = stationRepository.getStationById(departure.getDestinationId()); //departure.getDestination();
        final IdSet<Route> dropoffsAtDepDest = departureDestination.getDropoffRoutes().stream().collect(IdSet.collector());
        return !IdSet.intersection(dropOffsAtDest, dropoffsAtDepDest).isEmpty();
    }
}
