package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.RunningRoutesAndServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

public class JourneyConstraints {

    private static final int TRAMS_MAX_PATH_LENGTH = 400; // likely bigger than needed, but does not impact performance

    // TODO Spike based on Route lengths
    private static final int BUSES_MAX_PATH_LENGTH = 605; // todo right value?
    private static final int TRAINS_MAX_PATH_LENGTH = 2000; // todo right value?
    private static final int FERRY_MAX_PATH_LENGTH = 200; // todo right value?
    private static final int SUBWAY_MAX_PATH_LENGTH = 400; // todo right value?

    private static final Logger logger = LoggerFactory.getLogger(JourneyConstraints.class);

    private final RunningRoutesAndServices.FilterForDate routesAndServicesFilter;
    private final TimeRange destinationsAvailable;
    private final TramchesterConfig config;
    private final int maxPathLength;
    private final IdSet<Station> closedStationsIds;
    private final Set<ClosedStation> closedStations;
    private final Duration maxJourneyDuration;
    private final int maxWalkingConnections;
    private final int maxNumberWalkingConnections;
    private final LowestCostsForDestRoutes lowestCostForDestinations;
    private final EnumSet<TransportMode> destinationModes; // must account for interchange

    public JourneyConstraints(TramchesterConfig config, RunningRoutesAndServices.FilterForDate routesAndServicesFilter,
                              Set<ClosedStation> closedStations, EnumSet<TransportMode> destinationModes,
                              LowestCostsForDestRoutes lowestCostForDestinations, Duration maxJourneyDuration,
                              TimeRange destinationsAvailable) {
        this.config = config;
        this.closedStations = closedStations;
        this.lowestCostForDestinations = lowestCostForDestinations;
        this.routesAndServicesFilter = routesAndServicesFilter;
        this.destinationsAvailable = destinationsAvailable;
        this.maxPathLength = computeMaxPathLength();

        //this.destinations = destinations;
        this.destinationModes = destinationModes;
        this.maxJourneyDuration = maxJourneyDuration;
        this.maxWalkingConnections = config.getMaxWalkingConnections();

        this.maxNumberWalkingConnections = config.getMaxWalkingConnections();

        this.closedStationsIds = closedStations.stream().map(ClosedStation::getStationId).collect(IdSet.idCollector());

        if (!closedStationsIds.isEmpty()) {
            logger.info("Have closed stations " + closedStationsIds);
        }

    }

    private int computeMaxPathLength() {
        return config.getTransportModes().stream().map(this::getPathMaxFor).max(Integer::compareTo).orElseThrow();
    }

    private int getPathMaxFor(TransportMode mode) {
        return switch (mode) {
            case Tram -> TRAMS_MAX_PATH_LENGTH;
            case Bus -> BUSES_MAX_PATH_LENGTH;
            case Train, RailReplacementBus -> TRAINS_MAX_PATH_LENGTH;
            case Subway -> SUBWAY_MAX_PATH_LENGTH;
            case Ferry -> FERRY_MAX_PATH_LENGTH;
            default -> throw new RuntimeException("Unexpected transport mode " + mode);
        };
    }


    public int getMaxPathLength() {
        return maxPathLength;
    }

//    public LocationCollection getDestinations() {
//        return destinations;
//    }

    public Duration getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    // TODO Only whole day for now
    public boolean isClosed(final IdFor<Station> stationId) {
        if (!closedStationsIds.contains(stationId)) {
            return false;
        }
        return closedStations.stream().
                filter(closedStation -> closedStation.getStationId().equals(stationId)).
                allMatch(ClosedStation::closedWholeDay);
    }

    public int getMaxWalkingConnections() {
        return maxWalkingConnections;
    }

    public LowestCostsForDestRoutes getFewestChangesCalculator() {
        return lowestCostForDestinations;
    }

    @Override
    public String toString() {
        return "JourneyConstraints{" +
                "runningServices=" + routesAndServicesFilter +
                ", maxPathLength=" + maxPathLength +
                //", endStations=" + destinations +
                ", closedStations=" + closedStationsIds +
                ", maxJourneyDuration=" + maxJourneyDuration +
                ", maxWalkingConnections=" + maxWalkingConnections +
                ", maxNeighbourConnections=" + maxNumberWalkingConnections +
                '}';
    }

    public boolean isUnavailable(final Route route, final TramTime visitTime) {
        return !routesAndServicesFilter.isRouteRunning(route.getId(), visitTime.isNextDay());
    }

    // time and date separate for performance reasons
    public boolean isRunningOnDate(final IdFor<Service> serviceId, final TramTime visitTime) {
        return routesAndServicesFilter.isServiceRunningByDate(serviceId, visitTime.isNextDay());
    }

    // time and date separate for performance reasons
    public boolean isRunningAtTime(final IdFor<Service> serviceId, final TramTime time, final int maxWait) {
        return routesAndServicesFilter.isServiceRunningByTime(serviceId, time, maxWait);
    }

    public boolean destinationsAvailable(final TramTime time) {
        if (destinationsAvailable.contains(time)) {
            return true;
        }
        final TramTime end = destinationsAvailable.getEnd();
        if (end.isNextDay()) {
            TramTime realEnd = destinationsAvailable.forFollowingDay().getEnd();
            return !time.isAfter(realEnd);
        } else {
            if (time.isAfter(end)) {
                return false;
            }
        }


        // todo logic on earliest?

        return true;
    }

    public EnumSet<TransportMode> getDestinationModes() {
        return destinationModes;
    }
}
