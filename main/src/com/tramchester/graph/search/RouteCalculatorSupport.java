package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
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

    protected RouteCalculatorSupport(PathToStages pathToStages, NodeContentsRepository nodeContentsRepository,
                                     GraphDatabase graphDatabaseService, TraversalStateFactory traversalStateFactory,
                                     ProvidesNow providesNow, MapPathToLocations mapPathToLocations,
                                     StationRepository stationRepository, TramchesterConfig config, TripRepository tripRepository,
                                     BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz) {
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
    }


    protected GraphNode getLocationNodeSafe(GraphTransaction txn, Location<?> location) {
        GraphNode stationNode = txn.findNode(location);
        if (stationNode == null) {
            String msg = "Unable to find node for " + location.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationNode;
    }

    @NotNull
    public Set<GraphNodeId> getDestinationNodeIds(LocationSet destinations) {
        Set<GraphNodeId> destinationNodeIds;
        try(GraphTransaction txn = graphDatabaseService.beginTx()) {
            destinationNodeIds = destinations.stream().
                    map(location -> getLocationNodeSafe(txn, location)).
                    map(GraphNode::getId).
                    collect(Collectors.toSet());
        }
        return destinationNodeIds;
    }

    @NotNull
    protected Stream<Integer> numChangesRange(JourneyRequest journeyRequest, NumberOfChanges computedChanges) {
        final int requestedMaxChanges = journeyRequest.getMaxChanges();

        final int computedMaxChanges = computedChanges.getMax();
        final int computedMinChanges = computedChanges.getMin();

        if (requestedMaxChanges < computedMinChanges) {
            logger.error(format("Requested max changes (%s) is less than computed minimum changes (%s) needed",
                    requestedMaxChanges, computedMaxChanges));
        }

        if (computedMaxChanges > requestedMaxChanges) {
            logger.info(format("Will exclude some routes, requests changes %s is less then computed max changes %s",
                    requestedMaxChanges, computedMaxChanges));
        }

        int max = Math.min(computedMaxChanges, requestedMaxChanges);
        int min = Math.min(computedMinChanges, requestedMaxChanges);

        logger.info("Will check journey from " + min + " to " + max +" changes. Computed was " + computedChanges);
        return IntStream.rangeClosed(min, max).boxed();
    }

    @NotNull
    private ServiceHeuristics createHeuristics(TramTime actualQueryTime, JourneyConstraints journeyConstraints, int maxNumChanges) {
        return new ServiceHeuristics(stationRepository, nodeContentsRepository, journeyConstraints, actualQueryTime,
                maxNumChanges);
    }

    public Stream<RouteCalculator.TimedPath> findShortestPath(GraphTransaction txn, Set<GraphNodeId> destinationNodeIds,
                                                              final LocationSet endStations,
                                                              ServiceReasons reasons, PathRequest pathRequest,
                                                              PreviousVisits previousSuccessfulVisit,
                                                              LowestCostSeen lowestCostSeen) {

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(
                txn, pathRequest, nodeContentsRepository,
                tripRepository, traversalStateFactory, endStations, config, destinationNodeIds,
                reasons, reasonToGraphViz, providesNow);

        logger.info("Traverse for " + pathRequest);

        return tramNetworkTraverser.
                findPaths(txn, pathRequest.startNode, previousSuccessfulVisit, lowestCostSeen, pathRequest.selector).
                map(path -> new RouteCalculator.TimedPath(path, pathRequest.queryTime, pathRequest.numChanges));
    }

    @NotNull
    protected Journey createJourney(JourneyRequest journeyRequest, RouteCalculator.TimedPath path,
                                    LocationSet destinations, AtomicInteger journeyIndex,
                                    GraphTransaction txn) {

        final List<TransportStage<?, ?>> stages = pathToStages.mapDirect(path, journeyRequest, destinations, txn);
        final List<Location<?>> locationList = mapPathToLocations.mapToLocations(path.path(), txn);

        if (stages.isEmpty()) {
            logger.error("No stages were mapped for " + journeyRequest + " for " + locationList);
        }
        final TramTime arrivalTime = getArrivalTimeFor(stages, journeyRequest);
        final TramTime departTime = getDepartTimeFor(stages, journeyRequest);
        logger.info("Created journey with " + stages.size() + " stages and dpeart time of " + departTime);
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
    protected ServiceReasons createServiceReasons(JourneyRequest journeyRequest, TramTime time) {
        return new ServiceReasons(journeyRequest, time, providesNow);
    }

    @NotNull
    protected ServiceReasons createServiceReasons(JourneyRequest journeyRequest, PathRequest pathRequest) {
        return new ServiceReasons(journeyRequest, pathRequest.queryTime, providesNow);
    }

    protected Duration getMaxDurationFor(JourneyRequest journeyRequest) {
        return journeyRequest.getMaxJourneyDuration();

    }

    public PathRequest createPathRequest(GraphNode startNode, TramDate queryDate, TramTime actualQueryTime,
                                         EnumSet<TransportMode> requestedModes, int numChanges,
                                         JourneyConstraints journeyConstraints, Duration maxInitialWait,
                                         BranchOrderingPolicy selector, boolean depthFirst) {
        final ServiceHeuristics serviceHeuristics = createHeuristics(actualQueryTime, journeyConstraints, numChanges);
        return new PathRequest(startNode, queryDate, actualQueryTime, numChanges, serviceHeuristics, requestedModes, maxInitialWait, selector, depthFirst);
    }

    public static class PathRequest {
        private final GraphNode startNode;
        private final TramTime queryTime;
        private final int numChanges;
        private final ServiceHeuristics serviceHeuristics;
        private final TramDate queryDate;
        private final EnumSet<TransportMode> requestedModes;
        private final Duration maxInitialWait;
        public final BranchOrderingPolicy selector;
        private final boolean depthFirst;

        public PathRequest(GraphNode startNode, TramDate queryDate, TramTime queryTime, int numChanges,
                           ServiceHeuristics serviceHeuristics, EnumSet<TransportMode> requestedModes,
                           Duration maxInitialWait, BranchOrderingPolicy selector, boolean depthFirst) {
            this.startNode = startNode;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.numChanges = numChanges;
            this.serviceHeuristics = serviceHeuristics;
            this.requestedModes = requestedModes;
            this.maxInitialWait = maxInitialWait;
            this.selector = selector;
            this.depthFirst = depthFirst;
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

        public boolean getDepthFirst() {
            return depthFirst;
        }
    }

    public static Duration getMaxInitialWaitFor(final Location<?> location, final TramchesterConfig config) {
        final DataSourceID dataSourceID = location.getDataSourceID();
        return config.getInitialMaxWaitFor(dataSourceID);
    }

    public static Duration getMaxInitialWaitFor(final Set<StationWalk> stationWalks, final TramchesterConfig config) {
        final Optional<Duration> longestWait = stationWalks.stream().
                map(StationWalk::getStation).
                map(station -> getMaxInitialWaitFor(station, config)).
                max(Duration::compareTo);
        if (longestWait.isEmpty()) {
            throw new RuntimeException("Could not compute inital max wait for " + stationWalks);
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


}
