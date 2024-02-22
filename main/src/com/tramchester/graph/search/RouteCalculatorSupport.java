package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.diagnostics.CreateFailedJourneyDiagnostics;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorSupport {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorSupport.class);

    private final PathToStages pathToStages;
    private final GraphDatabase graphDatabaseService;
    protected final ProvidesNow providesNow;
    private final MapPathToLocations mapPathToLocations;
    private final StationRepository stationRepository;
    private final TramchesterConfig config;
    private final TripRepository tripRepository;
    private final TraversalStateFactory traversalStateFactory;
    protected final BetweenRoutesCostRepository routeToRouteCosts;
    private final NodeContentsRepository nodeContentsRepository;
    private final ReasonsToGraphViz reasonToGraphViz;
    private final CreateFailedJourneyDiagnostics failedJourneyDiagnostics;
    private final StationAvailabilityRepository stationAvailabilityRepository;
    private final boolean fullLogging; // turn down logging for grid searches

    protected RouteCalculatorSupport(PathToStages pathToStages, NodeContentsRepository nodeContentsRepository,
                                     GraphDatabase graphDatabaseService, TraversalStateFactory traversalStateFactory,
                                     ProvidesNow providesNow, MapPathToLocations mapPathToLocations,
                                     StationRepository stationRepository, TramchesterConfig config, TripRepository tripRepository,
                                     BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz, CreateFailedJourneyDiagnostics failedJourneyDiagnostics, StationAvailabilityRepository stationAvailabilityRepository, boolean fullLogging) {
        this.pathToStages = pathToStages;
        this.nodeContentsRepository = nodeContentsRepository;
        this.graphDatabaseService = graphDatabaseService;
        this.traversalStateFactory = traversalStateFactory;
        this.providesNow = providesNow;
        this.mapPathToLocations = mapPathToLocations;
        this.stationRepository = stationRepository;
        this.config = config;
        this.tripRepository = tripRepository;
        this.routeToRouteCosts = routeToRouteCosts;
        this.reasonToGraphViz = reasonToGraphViz;
        this.failedJourneyDiagnostics = failedJourneyDiagnostics;
        this.stationAvailabilityRepository = stationAvailabilityRepository;
        this.fullLogging = fullLogging;
    }


    protected GraphNode getLocationNodeSafe(final GraphTransaction txn, final Location<?> location) {
        final GraphNode findNode = txn.findNode(location);
        if (findNode == null) {
            String msg = "Unable to find node for " + location.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        logger.info("found node " + findNode.getId() + " for " + location.getId());
        return findNode;
    }

    @NotNull
    public Set<GraphNodeId> getDestinationNodeIds(final LocationCollection destinations) {
        final Set<GraphNodeId> destinationNodeIds;
        try(GraphTransaction txn = graphDatabaseService.beginTx()) {
            destinationNodeIds = destinations.locationStream().
                    map(location -> getLocationNodeSafe(txn, location)).
                    map(GraphNode::getId).
                    collect(Collectors.toSet());
        }
        return destinationNodeIds;
    }

    @NotNull
    protected Stream<Integer> numChangesRange(final JourneyRequest journeyRequest, final NumberOfChanges computedChanges) {
        final int requestedMaxChanges = journeyRequest.getMaxChanges();

        final int computedMaxChanges = computedChanges.getMax();
        final int computedMinChanges = computedChanges.getMin();

        if (fullLogging) {
            if (requestedMaxChanges < computedMinChanges) {
                logger.error(format("Requested max changes (%s) is less than computed minimum changes (%s) needed",
                        requestedMaxChanges, computedMaxChanges));
            }

            if (computedMaxChanges > requestedMaxChanges) {
                logger.info(format("Will exclude some routes, requests changes %s is less then computed max changes %s",
                        requestedMaxChanges, computedMaxChanges));
            }
        }

        final int max = Math.min(computedMaxChanges, requestedMaxChanges);
        final int min = Math.min(computedMinChanges, requestedMaxChanges);

        if (fullLogging) {
            logger.info("Will check journey from " + min + " to " + max + " changes. Computed was " + computedChanges);
        }
        return IntStream.rangeClosed(min, max).boxed();
    }

    public Stream<RouteCalculator.TimedPath> findShortestPath(final GraphTransaction txn, final ServiceReasons reasons, final PathRequest pathRequest,
                                                              final PreviousVisits previousSuccessfulVisit, final LowestCostSeen lowestCostSeen,
                                                              final LocationCollection destinations, final Set<GraphNodeId> destinationNodeIds,
                                                              final Running running) {
        if (fullLogging) {
            if (config.getDepthFirst()) {
                logger.info("Depth first is enabled. Traverse for " + pathRequest);
            } else {
                logger.info("Breadth first is enabled. Traverse for " + pathRequest);
            }
        }

        final TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(txn, nodeContentsRepository,
                tripRepository, traversalStateFactory, config, reasonToGraphViz, fullLogging);

        final Stream<Path> paths = tramNetworkTraverser.findPaths(txn, pathRequest, previousSuccessfulVisit, reasons, lowestCostSeen,
                destinationNodeIds, destinations, running);
        return paths.map(path -> new RouteCalculator.TimedPath(path, pathRequest));
    }

    @NotNull
    protected Journey createJourney(final JourneyRequest journeyRequest, final RouteCalculator.TimedPath path,
                                    final LocationCollection destinations, final AtomicInteger journeyIndex,
                                    final GraphTransaction txn) {

        final List<TransportStage<?, ?>> stages = pathToStages.mapDirect(path, journeyRequest, destinations, txn, fullLogging);
        final List<Location<?>> locationList = mapPathToLocations.mapToLocations(path.path(), txn);

        if (stages.isEmpty()) {
            logger.error("No stages were mapped for " + journeyRequest + " for " + locationList);
        }

        final TramTime arrivalTime = getArrivalTimeFor(stages, journeyRequest);
        final TramTime departTime = getDepartTimeFor(stages, journeyRequest);
        if (fullLogging) {
            logger.info("Created journey with " + stages.size() + " stages and depart time of " + departTime);
        }
        return new Journey(departTime, path.queryTime(), arrivalTime, stages, locationList, path.numChanges(),
                journeyIndex.getAndIncrement());
    }

    private TramTime getDepartTimeFor(final List<TransportStage<?, ?>> stages, final JourneyRequest journeyRequest) {
        if (stages.isEmpty()) {
            logger.warn("No stages were mapped, can't get depart time");
            return journeyRequest.getOriginalTime();
        } else {
            final TransportStage<?, ?> firstStage = stages.get(0);
            return firstStage.getFirstDepartureTime();
        }
    }

    private TramTime getArrivalTimeFor(final List<TransportStage<?, ?>> stages, final JourneyRequest journeyRequest) {
        final int size = stages.size();
        if (size == 0) {
            logger.warn("No stages were mapped, can't get arrival time");
            return journeyRequest.getOriginalTime();
        } else {
            final TransportStage<?, ?> lastStage = stages.get(size - 1);
            return lastStage.getExpectedArrivalTime();
        }
    }

    protected PreviousVisits createPreviousVisits() {
        return new PreviousVisits();
    }

    @NotNull
    protected ServiceReasons createServiceReasons(JourneyRequest journeyRequest) {
        return new ServiceReasons(journeyRequest, journeyRequest.getOriginalTime(), providesNow, failedJourneyDiagnostics);
    }

    @NotNull
    protected ServiceReasons createServiceReasons(final JourneyRequest journeyRequest, final PathRequest pathRequest) {
        return new ServiceReasons(journeyRequest, pathRequest.queryTime, providesNow, failedJourneyDiagnostics);
    }

    protected Duration getMaxDurationFor(JourneyRequest journeyRequest) {
        return journeyRequest.getMaxJourneyDuration();

    }

    public PathRequest createPathRequest(final JourneyRequest journeyRequest, final NodeAndStation nodeAndStation, final int numChanges,
                                         final JourneyConstraints journeyConstraints, final BranchOrderingPolicy selector) {
        final Duration maxInitialWait = getMaxInitialWaitFor(nodeAndStation.location, config);
        final ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeContentsRepository, journeyConstraints,
                journeyRequest.getOriginalTime(), numChanges);
        return new PathRequest(journeyRequest, nodeAndStation.node, numChanges, serviceHeuristics, maxInitialWait, selector);
    }

    public PathRequest createPathRequest(GraphNode startNode, TramDate queryDate, TramTime actualQueryTime,
                                         EnumSet<TransportMode> requestedModes, int numChanges,
                                         JourneyConstraints journeyConstraints, Duration maxInitialWait,
                                         BranchOrderingPolicy selector) {
        final ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, nodeContentsRepository, journeyConstraints,
                actualQueryTime, numChanges);
        return new PathRequest(startNode, queryDate, actualQueryTime, numChanges, serviceHeuristics, requestedModes, maxInitialWait, selector);
    }

    protected TimeRange getDestinationsAvailable(LocationCollection destinations, TramDate tramDate) {
        return stationAvailabilityRepository.getAvailableTimesFor(destinations, tramDate);
    }

    public static class PathRequest {
        private final GraphNode startNode;
        private final TramTime queryTime;
        private final int numChanges;
        private final ServiceHeuristics serviceHeuristics;
        private final TramDate queryDate;
        private final EnumSet<TransportMode> requestedModes;
        private final Duration maxInitialWait;
        private final BranchOrderingPolicy selector;

        public PathRequest(JourneyRequest journeyRequest, GraphNode startNode, int numChanges, ServiceHeuristics serviceHeuristics,
                           Duration maxInitialWait, BranchOrderingPolicy selector) {
            this(startNode, journeyRequest.getDate(), journeyRequest.getOriginalTime(), numChanges, serviceHeuristics, journeyRequest.getRequestedModes(),
                    maxInitialWait, selector);

        }

        public PathRequest(GraphNode startNode, TramDate queryDate, TramTime queryTime, int numChanges,
                           ServiceHeuristics serviceHeuristics, EnumSet<TransportMode> requestedModes,
                           Duration maxInitialWait, BranchOrderingPolicy selector) {
            this.startNode = startNode;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.numChanges = numChanges;
            this.serviceHeuristics = serviceHeuristics;
            this.requestedModes = requestedModes;
            this.maxInitialWait = maxInitialWait;
            this.selector = selector;
        }

        public ServiceHeuristics getServiceHeuristics() {
            return serviceHeuristics;
        }

        public TramTime getActualQueryTime() {
            return queryTime;
        }

        public int getNumChanges() {
            return numChanges;
        }

        @Override
        public String toString() {
            return "PathRequest{" +
                    "startNode=" + startNode +
                    ", queryTime=" + queryTime +
                    ", numChanges=" + numChanges +
                    ", serviceHeuristics=" + serviceHeuristics +
                    ", queryDate=" + queryDate +
                    ", requestedModes=" + requestedModes +
                    ", maxInitialWait=" + maxInitialWait +
                    '}';
        }

        public TramDate getQueryDate() {
            return queryDate;
        }

        public EnumSet<TransportMode> getRequestedModes() {
            return requestedModes;
        }

        public Duration getMaxInitialWait() {
            return maxInitialWait;
        }

        public GraphNode getStartNode() {
            return startNode;
        }

        public BranchOrderingPolicy getSelector() {
            return selector;
        }
    }

    public static Duration getMaxInitialWaitFor(final Location<?> location, final TramchesterConfig config) {
        return config.getInitialMaxWaitFor(location.getDataSourceID());
    }

    public static Duration getMaxInitialWaitFor(final Set<StationWalk> stationWalks, final TramchesterConfig config) {
        final Optional<Duration> longestWait = stationWalks.stream().
                map(StationWalk::getStation).
                map(station -> getMaxInitialWaitFor(station, config)).
                max(Duration::compareTo);
        if (longestWait.isEmpty()) {
            throw new RuntimeException("Could not compute initial max wait for " + stationWalks);
        }
        return longestWait.get();
    }

    public static class InitialWalksFinished {

        private final long maxJourneys;
        private int seenMaxJourneys;

        private final Map<Location<?>, AtomicLong> journeysPerStation;

        public InitialWalksFinished(JourneyRequest journeyRequest, Set<StationWalk> stationWalks) {
            this.maxJourneys = journeyRequest.getMaxNumberOfJourneys();
            journeysPerStation = new HashMap<>();

            seenMaxJourneys = 0;
            stationWalks.stream().map(StationWalk::getStation).forEach(station -> journeysPerStation.put(station, new AtomicLong()));

        }

        public boolean notDoneYet(Journey journey) {
            if (!(journey.firstStageIsWalk() || journey.firstStageIsConnect())) {
                throw new RuntimeException("Expected walk to be first stage of " + journey);
            }

            TransportStage<?, ?> walkingStage = journey.getStages().get(0);

            final Location<?> lastStation = walkingStage.getLastStation();
            long countForStation = journeysPerStation.get(lastStation).incrementAndGet();
            if (countForStation==maxJourneys) {
                logger.info("Seen " + maxJourneys + " for " + lastStation.getId());
                seenMaxJourneys = seenMaxJourneys + 1;
            }
            return seenMaxJourneys < journeysPerStation.size();

        }
    }

    @NotNull
    protected NodeAndStation createNodeAndStation(GraphTransaction txn, Location<?> start) {
        return new NodeAndStation(start, getLocationNodeSafe(txn, start));
    }

    public record NodeAndStation(Location<?> location, GraphNode node) {

    }


}
