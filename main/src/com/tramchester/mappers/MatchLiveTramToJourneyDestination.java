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
                                                    final IdFor<Station> journeyDestinationId) {
        return switch (upcomingDeparture.getMode()) {
            case Tram -> matchesJourneyDestinationForTram(upcomingDeparture, origChangeStationsId, journeyDestinationId);
            case Train, RailReplacementBus -> matchesJourneyDestinationForTrain(upcomingDeparture, origChangeStationsId, journeyDestinationId);
            case Ferry, Subway, Bus, Ship -> false;
            case Walk, Connect, NotSet, Unknown -> throw new RuntimeException("Unexpected mode for an UpcomingDeparture " + upcomingDeparture);
        };
    }

    public boolean matchesJourneyDestinationForTram(final UpcomingDeparture dueTram, final IdSet<Station> origChangeStationsId,
                                             final IdFor<Station> journeyDestinationId) {

        // this should no longer happen...todo except for tests....?
        final IdSet<Station> changeStationIds;
        if (origChangeStationsId.contains(journeyDestinationId)) {
            logger.warn("original destinations " + origChangeStationsId + " incorrectly contains "
                    + journeyDestinationId + " so removing");
            changeStationIds = IdSet.copy(origChangeStationsId);
            changeStationIds.remove(journeyDestinationId);
        } else {
            changeStationIds = origChangeStationsId;
        }

        final IdFor<Station> dueDestinationId = dueTram.getDestinationId();

        if (journeyDestinationId.equals(dueDestinationId)) {
            // quick win, tram is going to our final destination
            return true;
        }

        final Station displayLocation = dueTram.getDisplayLocation();
        final TramDate date = TramDate.of(dueTram.getDate());

        // check for trams "towards" our destination
        final Station journeyDestination = stationRepository.getStationById(journeyDestinationId);
        final IdSet<Route> journeyDestinationDropOffs = journeyDestination.getDropoffRoutes().stream().collect(IdSet.collector());

        final Station dueTramFinalDestination = stationRepository.getStationById(dueTram.getDestinationId());

        if (anyRouteOverlap(dueTramFinalDestination, journeyDestinationDropOffs)) {
            final boolean callsAtDest = stopOrderChecker.check(date, displayLocation, journeyDestinationId, dueDestinationId) ||
                    stopOrderChecker.check(date, displayLocation, dueDestinationId, journeyDestinationId);
            if (callsAtDest) {
                return true; // else check on change stations
            }
        }

        // match on change stations?

        final Set<Station> changeStations = changeStationIds.stream().
                map(stationRepository::getStationById).
                collect(Collectors.toSet());

        boolean callsAtChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(dueTramFinalDestination, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, callingStation.getId(), dueDestinationId));

        if (callsAtChangeStation) {
            return true;
        }

        boolean towardsChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(dueTramFinalDestination, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, dueDestinationId, callingStation.getId()));

        if (towardsChangeStation) {
            return true;
        }

        // todo into debug
        logger.info("Did not match due tram " + dueTram + " with any of " + changeStationIds + " or " + journeyDestinationId);
        return false;

    }

    private boolean anyRouteOverlap(final Station stationA, final Station stationB) {
        final IdSet<Route> dropOffsA = stationA.getDropoffRoutes().stream().collect(IdSet.collector());
        final IdSet<Route> dropOffsB = stationB.getDropoffRoutes().stream().collect(IdSet.collector());

        return IdSet.anyOverlap(dropOffsA, dropOffsB);
    }

    private static boolean anyRouteOverlap(final Station station, final IdSet<Route> routesToCheck) {
        final IdSet<Route> dropOffs = station.getDropoffRoutes().stream().collect(IdSet.collector());
        return IdSet.anyOverlap(routesToCheck, dropOffs);
    }

    private boolean matchesJourneyDestinationForTrain(UpcomingDeparture upcomingDeparture, IdSet<Station> origChangeStationsId, IdFor<Station> journeyDestinationId) {
        return false;
    }
}
