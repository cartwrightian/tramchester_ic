package com.tramchester.resources;

import com.google.inject.Inject;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.graph.search.RouteCalculatorSupport;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.mappers.Geography;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_FROM_STATION;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
import static java.lang.String.format;

@LazySingleton
public class LocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationJourneyPlanner.class);

    private final StationLocationsRepository stationLocations;
    private final GraphFilter graphFilter;
    private final TramchesterConfig config;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final NodeContentsRepository nodeOperations;
    private final MarginInMeters margin;
    private final BetweenRoutesCostRepository routeToRouteCosts;
    private final Geography geography;

    @Inject
    public LocationJourneyPlanner(StationLocations stationLocations, TramchesterConfig config, RouteCalculator routeCalculator,
                                  RouteCalculatorArriveBy routeCalculatorArriveBy, NodeContentsRepository nodeOperations,
                                  GraphFilter graphFilter, RouteToRouteCosts routeToRouteCosts, Geography geography) {
        logger.info("created");

        this.geography = geography;
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.nodeOperations = nodeOperations;
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
        final ImmutableGraphTransaction immutableTxn = txn.asImmutable();
        if (journeyRequest.getArriveBy()) {
            return routeCalculatorArriveBy.calculateRoute(immutableTxn, start, destination, journeyRequest, running);
        } else {
            return routeCalculator.calculateRoute(immutableTxn, start, destination, journeyRequest, running);
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

        final WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, nodeOperations);
        final MutableGraphNode startOfWalkNode = nodesAndRelationships.createWalkingNode(start, journeyRequest);
        nodesAndRelationships.createWalksToStart(startOfWalkNode, walksToStart);

        final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(walksToStart, config);
        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        final int numberOfChanges = findNumberChangesWalkAtStart(walksToStart, destination, journeyRequest, timeRange);
        final Stream<Journey> journeys;
        Running running = () -> true;
        final ImmutableGraphTransaction immutable = txn.asImmutable();
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStart(immutable, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges, running);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStart(immutable, walksToStart, startOfWalkNode, destination, journeyRequest, numberOfChanges, running);
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

        final WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, nodeOperations);

        final MutableGraphNode endWalk = nodesAndRelationships.createWalkingNode(destination, journeyRequest);
        final List<MutableGraphRelationship> addedRelationships = new LinkedList<>();

        nodesAndRelationships.createWalksToDest(endWalk, walksToDest);

        nodesAndRelationships.addAll(addedRelationships);

        final LocationSet<Station> destinationStations = walksToDest.stream().map(StationWalk::getStation).collect(LocationSet.stationCollector());

        final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(start, config);
        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        final int numberOfChanges = findNumberChangesWalkAtEnd(start, walksToDest, journeyRequest, timeRange);

        final Stream<Journey> journeys;
        final Running running = () -> true;
        final ImmutableGraphTransaction immutable = txn.asImmutable();
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtEnd(immutable, start, endWalk, destinationStations, journeyRequest, numberOfChanges, running);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtEnd(immutable, start, endWalk, destinationStations, journeyRequest, numberOfChanges, running);
        }

        //noinspection ResultOfMethodCallIgnored
        journeys.onClose(nodesAndRelationships::delete);

        return journeys;
    }

    private Stream<Journey> quickestRouteWalkAtStartAndEnd(final MutableGraphTransaction txn, final Location<?> start, final Location<?> dest,
                                                           final JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s on %s", start, dest, journeyRequest));

        final WalkNodesAndRelationships nodesAndRelationships = new WalkNodesAndRelationships(txn, nodeOperations);

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

        final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(walksAtStart, config);
        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        final int numberOfChanges = findNumberChangesWalksStartAndEnd(walksAtStart, walksToDest, journeyRequest, timeRange);

        /// CALC
        Stream<Journey> journeys;
        Running running = () -> true;
        final ImmutableGraphTransaction immutable = txn.asImmutable();
        if (journeyRequest.getArriveBy()) {
            journeys = routeCalculatorArriveBy.calculateRouteWalkAtStartAndEnd(immutable, walksAtStart, startNode,  endWalk, destinationStations,
                    journeyRequest, numberOfChanges, running);
        } else {
            journeys = routeCalculator.calculateRouteWalkAtStartAndEnd(immutable, walksAtStart, startNode, endWalk, destinationStations,
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

    private Duration calculateDuration(Location<?> location, Station station) {
        return geography.getWalkingDuration(location, station);
    }

    private static class WalkNodesAndRelationships {

        private final NodeContentsRepository nodeOperations;
        private final MutableGraphTransaction txn;
        private final List<MutableGraphRelationship> relationships;
        private final List<MutableGraphNode> nodes;

        private WalkNodesAndRelationships(MutableGraphTransaction txn, NodeContentsRepository nodeOperations) {
            this.nodeOperations = nodeOperations;
            this.txn = txn;
            this.relationships = new ArrayList<>();
            this.nodes = new ArrayList<>();
        }

        public void delete() {
            logger.info("Removed added walks and walk node(s)");
            relationships.forEach(relationship -> {
                nodeOperations.deleteFromCostCache(relationship);
                relationship.delete();
            });
            for (MutableGraphNode node : nodes) {
                node.delete();
            }
        }

        public void addAll(List<MutableGraphRelationship> relationshipList) {
            relationships.addAll(relationshipList);
        }

        public MutableGraphNode createWalkingNode(Location<?> location, JourneyRequest journeyRequest) {
            MutableGraphNode walkingNode = createWalkingNode(txn, location.getLatLong(), journeyRequest.getUid());
            nodes.add(walkingNode);
            return walkingNode;
        }

        public void createWalksToStart(MutableGraphNode node, Set<StationWalk> walks) {
            createWalkRelationships(node, walks, WALKS_TO_STATION);
        }

        public void createWalksToDest(MutableGraphNode node, Set<StationWalk> walks) {
            createWalkRelationships(node, walks, WALKS_FROM_STATION);
        }

        private void createWalkRelationships(MutableGraphNode node, Set<StationWalk> walks, TransportRelationshipTypes direction) {
            List<MutableGraphRelationship> addedRelationships = new ArrayList<>();
            walks.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(node, stationWalk, direction)));
            relationships.addAll(addedRelationships);
        }

        private MutableGraphRelationship createWalkRelationship(MutableGraphNode walkNode, StationWalk stationWalk, TransportRelationshipTypes direction) {
            Station walkStation = stationWalk.getStation();
            Duration cost = stationWalk.getCost();

            MutableGraphRelationship walkingRelationship;
            MutableGraphNode stationNode = txn.findNodeMutable(walkStation);
            if (stationNode==null) {
                throw new RuntimeException("Could not find node for " + walkStation);
            }

            if (direction == WALKS_FROM_STATION) {
                walkingRelationship = stationNode.createRelationshipTo(txn, walkNode, direction);
                logger.info(format("Add %s relationship %s (%s) to node %s cost %s",
                        direction, walkStation.getId(), walkStation.getName(), walkNode.getId(),  cost));
            } else if (direction == WALKS_TO_STATION) {
                walkingRelationship = walkNode.createRelationshipTo(txn, stationNode, direction);
                logger.info(format("Add %s relationship between node %s to %s (%s) cost %s",
                        direction, walkNode.getId(), walkStation.getId(), walkStation.getName(), cost));
            } else {
                throw new RuntimeException("Unknown direction " + direction);
            }

            walkingRelationship.setCost(cost);
//            walkingRelationship.setMaxCost(cost);
            walkingRelationship.set(walkStation);
            return walkingRelationship;
        }

        private MutableGraphNode createWalkingNode(MutableGraphTransaction txn, LatLong origin, UUID uniqueId) {
            MutableGraphNode startOfWalkNode = txn.createNode(GraphLabel.QUERY_NODE);
            startOfWalkNode.setLatLong(origin);
            startOfWalkNode.setWalkId(origin, uniqueId);
            logger.info(format("Added walking node at %s as %s", origin, startOfWalkNode));
            return startOfWalkNode;
        }

    }

}
