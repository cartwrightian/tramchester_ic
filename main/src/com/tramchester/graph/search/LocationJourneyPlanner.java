package com.tramchester.graph.search;

import com.google.inject.Inject;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.mappers.Geography;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class LocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationJourneyPlanner.class);

    private final StationLocationsRepository stationLocations;
    private final GraphFilter graphFilter;
    private final TramchesterConfig config;
    private final TramRouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final MarginInMeters margin;
    private final BetweenRoutesCostRepository routeToRouteCosts;
    private final Geography geography;

    @Inject
    public LocationJourneyPlanner(StationLocations stationLocations, TramchesterConfig config, TramRouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, GraphFilter graphFilter,
                                  RouteToRouteCosts routeToRouteCosts, Geography geography) {
        logger.info("created");

        this.geography = geography;
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.stationLocations = stationLocations;
        this.graphFilter = graphFilter;
        this.margin = config.getWalkingDistanceRange();
        this.routeToRouteCosts = routeToRouteCosts;
    }

    public Stream<Journey> quickestRouteForLocation(final MutableGraphTransaction txn, final Location<?> start, final Location<?> destination,
                                                    final JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", start.getId(), destination.getId(), destination.getName(), journeyRequest));
        final boolean walkAtStart = start.getLocationType().isWalk();
        final boolean walkAtEnd = destination.getLocationType().isWalk();

        if (walkAtStart && walkAtEnd) {
            return quickestRouteWalkAtStartAndEnd(txn, start, destination, journeyRequest);
        }
        if (walkAtStart) {
            return quickRouteWalkAtStart(txn, start, destination, journeyRequest);
        }
        if (walkAtEnd) {
            return quickestRouteWalkAtEnd(txn, start, destination, journeyRequest);
        }

        // station => station
        final Running running = () -> true;
        if (journeyRequest.getArriveBy()) {
            return routeCalculatorArriveBy.calculateRoute(txn, start, destination, journeyRequest, running);
        } else {
            return routeCalculator.calculateRoute(txn, start, destination, journeyRequest, running);
        }
    }

    private Stream<Journey> quickRouteWalkAtStart(final MutableGraphTransaction txn, final Location<?> start, final Location<?> destination,
                                                  final JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s (%s) for %s", start.getId(),
                destination.getId(), destination.getName(), journeyRequest));

        final GridPosition startGrid = start.getGridPosition();
        if (!stationLocations.getActiveStationBounds().within(margin, startGrid)) {
            logger.warn(format("Start %s not within %s of station bounds %s", startGrid, margin, stationLocations.getActiveStationBounds()));
        }

        final Set<StationWalk> walksToStart = getStationWalks(start, journeyRequest.getRequestedModes());
        if (walksToStart.isEmpty()) {
            logger.warn("No walks to start found " +start);
            return Stream.empty();
        }

        final WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn);
        final MutableGraphNode startOfWalkNode = nodesAndRelationships.createWalkingNode(start, journeyRequest);
        nodesAndRelationships.createWalksToStart(startOfWalkNode, walksToStart);

        final TramDuration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(walksToStart, config);
        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        final int numberOfChanges = findNumberChangesWalkAtStart(walksToStart, destination, journeyRequest, timeRange);
        final Stream<Journey> journeys;
        Running running = () -> true;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges, running);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(txn, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges, running);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(nodesAndRelationships::delete);
        return journeys;
    }

    private Stream<Journey> quickestRouteWalkAtEnd(final MutableGraphTransaction txn, final Location<?> start, final Location<?> destination,
                                                   final JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s for %s", start.getId(), start.getName(),
                destination, journeyRequest));

        final GridPosition endGrid = destination.getGridPosition();
        if (!stationLocations.withinBounds(destination)) {
            logger.warn(format("Destination %s not within %s of station bounds %s", endGrid, margin, stationLocations.getActiveStationBounds()));
        }

        if (!stationLocations.getActiveStationBounds().contained(destination)) {
            logger.warn("Destination not within station bounds " + destination);
        }

        final Set<StationWalk> walksToDest = getStationWalks(destination, journeyRequest.getRequestedModes());
        if (walksToDest.isEmpty()) {
            logger.warn("Cannot find any walks from " + destination + " to stations");
            return Stream.empty();
        }

        final WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn);

        final MutableGraphNode endWalk = nodesAndRelationships.createWalkingNode(destination, journeyRequest);
        //final List<MutableGraphRelationship> addedRelationships = new LinkedList<>();

        nodesAndRelationships.createWalksToDest(endWalk, walksToDest);

        //nodesAndRelationships.addAll(addedRelationships);

        final LocationSet<Station> destinationStations = walksToDest.stream().
                map(StationWalk::getStation).
                collect(LocationSet.stationCollector());

        final TramDuration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(start, config);
        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        final int numberOfChanges = findNumberChangesWalkAtEnd(start, walksToDest, journeyRequest, timeRange);

        final Stream<Journey> journeys;
        final Running running = () -> true;
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest, numberOfChanges, running);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(txn, start, endWalk, destinationStations, journeyRequest, numberOfChanges, running);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(nodesAndRelationships::delete);

        return journeys;
    }

    private Stream<Journey> quickestRouteWalkAtStartAndEnd(final MutableGraphTransaction txn, final Location<?> start, final Location<?> dest,
                                                           final JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", start, dest, journeyRequest));

        final WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn);

        // Add Walk at the Start
        final Set<StationWalk> walksAtStart = getStationWalks(start, journeyRequest.getRequestedModes());
        final MutableGraphNode startNode = nodesAndRelationships.createWalkingNode(start, journeyRequest);
        nodesAndRelationships.createWalksToStart(startNode, walksAtStart);

        // Add Walks at the end
        final Set<StationWalk> walksToDest = getStationWalks(dest, journeyRequest.getRequestedModes());
        final MutableGraphNode endWalk = nodesAndRelationships.createWalkingNode(dest, journeyRequest);
        nodesAndRelationships.createWalksToDest(endWalk, walksToDest);

        // where destination walks take us
        final LocationSet<Station> destinationStations = walksToDest.stream().
                map(StationWalk::getStation).collect(LocationSet.stationCollector());

        final TramDuration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(walksAtStart, config);
        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        final int numberOfChanges = findNumberChangesWalksStartAndEnd(walksAtStart, walksToDest, journeyRequest, timeRange);

        /// CALC
        Stream<Journey> journeys;
        Running running = () -> true;
//        final GraphTransaction immutable = txn.asImmutable();
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStartAndEnd(txn, walksAtStart, startNode,  endWalk, destinationStations,
                    journeyRequest, numberOfChanges, running);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStartAndEnd(txn, walksAtStart, startNode, endWalk, destinationStations,
                    journeyRequest, numberOfChanges, running);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(nodesAndRelationships::delete);
        return journeys;
    }

    public Set<StationWalk> getStationWalks(Location<?> location, EnumSet<TransportMode> modes) {

        int maxResults = config.getNumOfNearestStopsForWalking();
        List<Station> nearbyStationsWithComposites = stationLocations.nearestStationsSorted(location, maxResults, margin, modes);

        if (nearbyStationsWithComposites.isEmpty()) {
            logger.warn(format("Failed to find stations within %s of %s", margin, location));
            return Collections.emptySet();
        }

        List<Station> filtered = nearbyStationsWithComposites.stream()
                .filter(graphFilter::shouldInclude).collect(Collectors.toList());

        Set<StationWalk> stationWalks = createWalks(location, filtered);
        logger.info(format("Stops within %s of %s are [%s]", maxResults, location, stationWalks));
        return stationWalks;
    }

    private int findNumberChangesWalkAtEnd(Location<?> start, Set<StationWalk> walksToDest, JourneyRequest journeyRequest, TimeRange timeRange) {
        final LocationSet<Station> destinations = walksToDest.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return  routeToRouteCosts.getNumberOfChanges(start, destinations, journeyRequest, timeRange);
    }

    private int findNumberChangesWalkAtStart(Set<StationWalk> walksToStart, Location<?> destination, JourneyRequest journeyRequest, TimeRange timeRange) {
        final LocationSet<Station> starts = walksToStart.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(starts, destination, journeyRequest, timeRange);
    }

    private int findNumberChangesWalksStartAndEnd(Set<StationWalk> walksAtStart, Set<StationWalk> walksToDest, JourneyRequest journeyRequest, TimeRange timeRange) {
        final LocationSet<Station> destinations = walksToDest.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        final LocationSet<Station> starts = walksAtStart.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());
        return routeToRouteCosts.getNumberOfChanges(starts, destinations, journeyRequest, timeRange);
    }

    private Set<StationWalk> createWalks(Location<?> location, List<Station> startStations) {
        return startStations.stream().
                map(station -> new StationWalk(station, calculateDuration(location, station))).
                collect(Collectors.toSet());
    }

    private TramDuration calculateDuration(Location<?> location, Station station) {
        return geography.getWalkingDuration(location, station);
    }

}
