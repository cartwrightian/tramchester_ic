package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class MatchDeparturesToJourneyDestination {
    private static final Logger logger = LoggerFactory.getLogger(MatchDeparturesToJourneyDestination.class);

    private final StationRepository stationRepository;
    private final StopOrderChecker stopOrderChecker;

    @Inject
    public MatchDeparturesToJourneyDestination(StationRepository stationRepository, StopOrderChecker stopOrderChecker) {
        this.stationRepository = stationRepository;
        this.stopOrderChecker = stopOrderChecker;
    }

    public boolean matchesJourneyDestination(UpcomingDeparture departure, DestinationAndCallingPoints destinationAndCallingPoints) {
        if (destinationAndCallingPoints.isNone()) {
            throw new RuntimeException("Called for None, with departure " + departure);
        }
        return matchesJourneyDestination(departure, destinationAndCallingPoints.destination(),
                destinationAndCallingPoints.callingPoints());
    }

    private boolean matchesJourneyDestination(final UpcomingDeparture upcomingDeparture, final IdFor<Station> destId, final ImmutableIdSet<Station> changeIds) {
        // transport mode here needs to include connecting stage....
        return switch (upcomingDeparture.getMode()) {
            case Tram -> matchesJourneyDestinationWhenAllWithinBounds(upcomingDeparture, changeIds, destId);
            case Train, RailReplacementBus -> hasCallingPoints(upcomingDeparture);
            case Ferry, Subway, Bus, Ship -> false;
            case Walk, Connect, NotSet, Unknown -> throw new RuntimeException("Unexpected mode for an UpcomingDeparture " + upcomingDeparture);
        };
    }

    public boolean matchesJourneyDestinationWhenAllWithinBounds(final UpcomingDeparture departure,
                                                                final ImmutableIdSet<Station> origChangeStationIds,
                                                                final IdFor<Station> destId) {

        // this should no longer happen...todo except for tests....?
        final ImmutableIdSet<Station> changeStationIds;
        if (origChangeStationIds.contains(destId)) {
            logger.warn("original destinations " + origChangeStationIds + " incorrectly contains " + destId + " so removing");
            changeStationIds = IdSet.copyThenRemove(origChangeStationIds, destId);
        } else {
            changeStationIds = origChangeStationIds;
        }

        final IdFor<Station> dueDestinationId = departure.getDestinationId();

        if (destId.equals(dueDestinationId)) {
            // quick win, tram is going to our final destination
            return true;
        }

        final Station displayLocation = departure.getDisplayLocation();
        final TramDate date = TramDate.of(departure.getDate());

        // check for trams "towards" our destination
        final Station journeyDestination = stationRepository.getStationById(destId);
        final IdSet<Route> journeyDestinationDropOffs = journeyDestination.getDropoffRoutes().stream().collect(IdSet.collector());

        final Station finalDestination = stationRepository.getStationById(departure.getDestinationId());

        if (anyRouteOverlap(finalDestination, journeyDestinationDropOffs)) {
            final boolean callsAtDest = stopOrderChecker.check(date, displayLocation, destId, dueDestinationId) ||
                    stopOrderChecker.check(date, displayLocation, dueDestinationId, destId);
            if (callsAtDest) {
                return true; // else check on change stations
            }
        }

        // match on change stations?

        final Set<Station> changeStations = changeStationIds.stream().
                map(stationRepository::getStationById).
                collect(Collectors.toSet());

        boolean callsAtChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(finalDestination, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, callingStation.getId(), dueDestinationId));

        if (callsAtChangeStation) {
            return true;
        }

        boolean towardsChangeStation = changeStations.stream().
                filter(callingStation -> anyRouteOverlap(finalDestination, callingStation)).
                anyMatch(callingStation -> stopOrderChecker.check(date, displayLocation, dueDestinationId, callingStation.getId()));

        if (towardsChangeStation) {
            return true;
        }

        logger.debug("Did not match due tram " + departure + " with any of " + changeStationIds + " or " + destId);
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

    private boolean hasCallingPoints(final UpcomingDeparture upcomingDeparture) {
        // for train ASSUME we already filtered the departures
        boolean hasCallingPoints = upcomingDeparture.hasCallingPoints();
        if (!hasCallingPoints) {
            logger.warn("No calling points!");
        }
        return hasCallingPoints;
    }


}
