package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.core.*;
import com.tramchester.graph.search.diagnostics.CreateJourneyDiagnostics;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
//import com.tramchester.graph.search.neo4j.selectors.BranchSelectorFactory;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.*;
import org.jetbrains.annotations.NotNull;
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

public abstract class RouteCalculatorSupport {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorSupport.class);
    protected final TramchesterConfig config;
    protected final ClosedStationsRepository closedStationsRepository;
    protected final CacheMetrics cacheMetrics;
    protected final InterchangeRepository interchangeRepository;
    protected final CreateQueryTimes createQueryTimes;
    protected final RunningRoutesAndServices runningRoutesAndServices;

    private final PathToStages pathToStages;
    private final GraphDatabase graphDatabaseService;
    protected final ProvidesNow providesNow;
    private final MapPathToLocations mapPathToLocations;
    private final StationRepository stationRepository;
    protected final BetweenRoutesCostRepository routeToRouteCosts;
    private final CreateJourneyDiagnostics failedJourneyDiagnostics;
    private final StationAvailabilityRepository stationAvailabilityRepository;
    private final boolean fullLogging; // turn down logging for grid searches
    private final NumberOfNodesAndRelationshipsRepository countsNodes;

    protected RouteCalculatorSupport(PathToStages pathToStages,
                                     GraphDatabase graphDatabaseService,
                                     ProvidesNow providesNow, MapPathToLocations mapPathToLocations,
                                     StationRepository stationRepository, TramchesterConfig config,
                                     BetweenRoutesCostRepository routeToRouteCosts,
                                     CreateJourneyDiagnostics failedJourneyDiagnostics, StationAvailabilityRepository stationAvailabilityRepository,
                                     NumberOfNodesAndRelationshipsRepository countsNodes, ClosedStationsRepository closedStationsRepository,
                                     CacheMetrics cacheMetrics, InterchangeRepository interchangeRepository, CreateQueryTimes createQueryTimes, RunningRoutesAndServices runningRoutesAndServices) {
        this.pathToStages = pathToStages;
        this.graphDatabaseService = graphDatabaseService;
        this.providesNow = providesNow;
        this.mapPathToLocations = mapPathToLocations;
        this.stationRepository = stationRepository;
        this.routeToRouteCosts = routeToRouteCosts;
        this.failedJourneyDiagnostics = failedJourneyDiagnostics;
        this.stationAvailabilityRepository = stationAvailabilityRepository;
        this.fullLogging = this instanceof RouteCalculatorForBoxes;
        this.countsNodes = countsNodes;
        this.config = config;
        this.closedStationsRepository = closedStationsRepository;
        this.cacheMetrics = cacheMetrics;
//        this.branchSelectorFactory = branchSelectorFactory;
        this.interchangeRepository = interchangeRepository;
        this.createQueryTimes = createQueryTimes;
        this.runningRoutesAndServices = runningRoutesAndServices;
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
    protected Stream<Integer> numChangesRange(final JourneyRequest journeyRequest, final int computedMinChanges) {
        final JourneyRequest.MaxNumberOfChanges requestedMaxChanges = journeyRequest.getMaxChanges();

        if (fullLogging) {
            if (requestedMaxChanges.get() < computedMinChanges) {
                logger.error(format("Requested max changes (%s) is less than computed minimum changes (%s) needed",
                        requestedMaxChanges, computedMinChanges));
            }
        }

        final int max = requestedMaxChanges.get();

        if (fullLogging) {
            logger.info("Will check journey from " + computedMinChanges + " to " + max + " changes.");
        }
        return IntStream.rangeClosed(computedMinChanges, max).boxed();
    }

    public Stream<TimedPath> findShortestPath(final GraphTransaction txn, final ServiceReasons reasons, final PathRequest pathRequest,
                                              final PreviousVisits previousSuccessfulVisit, final LowestCostSeen lowestCostSeen,
                                              final Running running, final TramNetworkTraverserFactory factory, TowardsDestination towardsDestination) {
        if (fullLogging) {
            if (config.getDepthFirst()) {
                logger.info("Depth first is enabled. Traverse for " + pathRequest);
            } else {
                logger.info("Breadth first is enabled. Traverse for " + pathRequest);
            }
        }

        final TramNetworkTraverser tramNetworkTraverser = factory.get(txn);

        final Stream<GraphPath> paths = tramNetworkTraverser.findPaths(pathRequest, previousSuccessfulVisit, reasons, lowestCostSeen,
                towardsDestination, running);

        return paths.map(path -> new TimedPath(path, pathRequest));
    }

    @NotNull
    protected Journey createJourney(final JourneyRequest journeyRequest, final TimedPath path,
                                    final TowardsDestination towardsDestination, final AtomicInteger journeyIndex,
                                    final GraphTransaction txn) {

        final List<TransportStage<?, ?>> stages = pathToStages.mapDirect(path, journeyRequest, towardsDestination, txn, fullLogging);
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
            final TransportStage<?, ?> firstStage = stages.getFirst();
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

    protected PreviousVisits createPreviousVisits(final JourneyRequest journeyRequest) {
        boolean cacheDisabled = config.getDepthFirst() || journeyRequest.getCachingDisabled();
        return new PreviousVisits(cacheDisabled, countsNodes);
    }

    @NotNull
    protected ServiceReasons createServiceReasons(final JourneyRequest journeyRequest) {
        return new ServiceReasons(journeyRequest, journeyRequest.getOriginalTime(), providesNow, failedJourneyDiagnostics);
    }

    @NotNull
    protected ServiceReasons createServiceReasons(final JourneyRequest journeyRequest, final PathRequest pathRequest) {
        return new ServiceReasons(journeyRequest, pathRequest.getActualQueryTime(), providesNow, failedJourneyDiagnostics);
    }

    public PathRequest createPathRequest(final JourneyRequest journeyRequest, final NodeAndStation nodeAndStation, final int numChanges,
                                         final JourneyConstraints journeyConstraints) {
        final Duration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(nodeAndStation.location, config);
        final ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, journeyConstraints,
                journeyRequest.getOriginalTime(), numChanges);
        return new PathRequest(journeyRequest, nodeAndStation.node, numChanges, serviceHeuristics, maxInitialWait,
                journeyConstraints.getDestinationModes());
    }

    public PathRequest createPathRequest(GraphNode startNode, TramDate queryDate, TramTime actualQueryTime,
                                         EnumSet<TransportMode> requestedModes, int numChanges,
                                         JourneyConstraints journeyConstraints, Duration maxInitialWait) {
        final ServiceHeuristics serviceHeuristics = new ServiceHeuristics(stationRepository, journeyConstraints,
                actualQueryTime, numChanges);
        return new PathRequest(startNode, queryDate, actualQueryTime, numChanges, serviceHeuristics, requestedModes, maxInitialWait,
                journeyConstraints.getDestinationModes());
    }

    protected TimeRange getDestinationsAvailable(LocationCollection destinations, TramDate tramDate) {
        return stationAvailabilityRepository.getAvailableTimesFor(destinations, tramDate);
    }

    public static Duration getMaxInitialWaitFor(List<? extends BoundingBoxWithStations> startingBoxes, TramchesterConfig config) {
        Optional<Duration> findMaxInitialWait = startingBoxes.stream().
                flatMap(box -> box.getStations().stream()).
                map(station -> TramchesterConfig.getMaxInitialWaitFor(station, config))
                .max(Duration::compareTo);
        if (findMaxInitialWait.isEmpty()) {
            throw new RuntimeException("Could not find max initial wait from " + startingBoxes);
        }
        return findMaxInitialWait.get();
    }

    protected EnumSet<TransportMode> resolveRealModes(final LocationCollection destinations) {
        // need to take into account if a location is an interchange
        final EnumSet<TransportMode> interchangeModes = interchangeRepository.getInterchangeModes(destinations);
        final EnumSet<TransportMode> results = EnumSet.copyOf(destinations.getModes());
        results.addAll(interchangeModes);
        return results;
    }

    protected Stream<Journey> getJourneyStream(final GraphTransaction txn, final GraphNode startNode, final GraphNode endNode,
                                               final LocationCollection destinations, final JourneyRequest journeyRequest,
                                               final List<TramTime> queryTimes, final RunningRoutesAndServices.FilterForDate runningRoutesAndServicesFilter,
                                               final int possibleMinNumChanges, final Duration maxInitialWait, final Running running) {

        if (possibleMinNumChanges==Integer.MAX_VALUE) {
            logger.error(format("Computed min number of changes is MAX_VALUE, journey %s is not possible?", journeyRequest));
            // todo fall back to requested max changes in journeyRequest ?
            return Stream.empty();
        }

        final EnumSet<TransportMode> requestedModes = journeyRequest.getRequestedModes();

        final Set<GraphNodeId> destinationNodeIds = Collections.singleton(endNode.getId());
        final TramDate tramDate = journeyRequest.getDate();

        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        final LowestCostsForDestRoutes lowestCostsForRoutes = routeToRouteCosts.getLowestCostCalculatorFor(destinations, journeyRequest, timeRange);

        final Duration maxJourneyDuration = journeyRequest.getMaxJourneyDuration();

        final Set<ClosedStation> closedStations = closedStationsRepository.getAnyWithClosure(tramDate);

        final EnumSet<TransportMode> destinationModes = resolveRealModes(destinations);

        final TimeRange destinationsAvailable = getDestinationsAvailable(destinations, tramDate);
        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, runningRoutesAndServicesFilter,
                closedStations, destinationModes, lowestCostsForRoutes, maxJourneyDuration, destinationsAvailable);

        logger.info("Journey Constraints: " + journeyConstraints);
        logger.info("Query times: " + queryTimes);

        final LowestCostSeen lowestCostSeen = new LowestCostSeen();

        final AtomicInteger journeyIndex = new AtomicInteger(0);

        final TowardsDestination towardsDestination = new TowardsDestination(destinations);

        final TramNetworkTraverserFactory traverserFactory = getTraverserFactory(destinations, destinationNodeIds);

        final Stream<Journey> results = numChangesRange(journeyRequest, possibleMinNumChanges).
                flatMap(numChanges -> queryTimes.stream().
                        map(queryTime -> createPathRequest(startNode, tramDate, queryTime, requestedModes, numChanges,
                                journeyConstraints, maxInitialWait))).
                flatMap(pathRequest -> findShortestPath(txn, createServiceReasons(journeyRequest, pathRequest), pathRequest,
                        createPreviousVisits(journeyRequest), lowestCostSeen, running, traverserFactory, towardsDestination)).
                map(path -> createJourney(journeyRequest, path, towardsDestination, journeyIndex, txn));

        //noinspection ResultOfMethodCallIgnored
        results.onClose(() -> {
            cacheMetrics.report();
            logger.info("Journey stream closed");
        });

        return results;
    }

    protected Stream<Journey> getSingleJourneyStream(final GraphTransaction txn, final GraphNode startNode, final GraphNode endNode,
                                                     final JourneyRequest journeyRequest, RunningRoutesAndServices.FilterForDate routesAndServicesFilter,
                                                     final LocationCollection destinations,
                                                     final Duration maxInitialWait, final Running running) {

        final TramDate tramDate = journeyRequest.getDate();
        final Set<GraphNodeId> destinationNodeIds = Collections.singleton(endNode.getId());

        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);
        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        final LowestCostsForDestRoutes lowestCostsForRoutes = routeToRouteCosts.getLowestCostCalculatorFor(destinations, journeyRequest, timeRange);

        final Duration maxJourneyDuration = journeyRequest.getMaxJourneyDuration();

        final Set<ClosedStation> closedStations = closedStationsRepository.getAnyWithClosure(tramDate);

        final TimeRange destinationsAvailable = getDestinationsAvailable(destinations, tramDate);

        final EnumSet<TransportMode> requestedModes = journeyRequest.getRequestedModes();

        final EnumSet<TransportMode> destinationModes = resolveRealModes(destinations);

        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, routesAndServicesFilter,
                closedStations, destinationModes, lowestCostsForRoutes, maxJourneyDuration, destinationsAvailable);

        logger.info("Journey Constraints: " + journeyConstraints);

        final LowestCostSeen lowestCostSeen = new LowestCostSeen();

        final AtomicInteger journeyIndex = new AtomicInteger(0);

        final PathRequest singlePathRequest = createPathRequest(startNode, tramDate, journeyRequest.getOriginalTime(), requestedModes,
                journeyRequest.getMaxChanges().get(),
                journeyConstraints, maxInitialWait);

        final ServiceReasons serviceReasons = createServiceReasons(journeyRequest, singlePathRequest);

        final TowardsDestination towardsDestination = new TowardsDestination(destinations);

        TramNetworkTraverserFactory traverserFactory = getTraverserFactory(destinations, destinationNodeIds);

        final Stream<Journey> results = findShortestPath(txn, serviceReasons, singlePathRequest,
                        createPreviousVisits(journeyRequest), lowestCostSeen, running, traverserFactory, towardsDestination).
                map(path -> createJourney(journeyRequest, path, towardsDestination, journeyIndex, txn));

        //noinspection ResultOfMethodCallIgnored
        results.onClose(() -> {
            cacheMetrics.report();
            logger.info("Journey stream closed");
        });

        return results;
    }

    public Stream<Journey> calculateRoute(final GraphTransaction txn, final Location<?> start, final Location<?> destination,
                                          final JourneyRequest journeyRequest, final Running running) {
        logger.info(format("Finding shortest path for %s (%s) --> %s (%s) for %s",
                start.getName(), start.getId(), destination.getName(), destination.getId(), journeyRequest));

        final GraphNode startNode = getLocationNodeSafe(txn, start);
        final GraphNode endNode = getLocationNodeSafe(txn, destination);

        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        final Duration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(start, config);

        final int numberOfChanges = getPossibleMinNumberOfChanges(start, destination, journeyRequest, maxInitialWait);

        final RunningRoutesAndServices.FilterForDate routesAndServicesFilter = runningRoutesAndServices.getFor(journeyRequest);

        final LocationCollection destinations = LocationCollectionSingleton.of(destination);
        if (journeyRequest.getDiagnosticsEnabled()) {
            logger.warn("Diagnostics enabled, will only query for single result");

            return getSingleJourneyStream(txn, startNode, endNode, journeyRequest, routesAndServicesFilter, destinations, maxInitialWait, running).
                    limit(journeyRequest.getMaxNumberOfJourneys());
        } else {
            return getJourneyStream(txn, startNode, endNode, destinations, journeyRequest, queryTimes, routesAndServicesFilter,
                    numberOfChanges, maxInitialWait, running).
                    limit(journeyRequest.getMaxNumberOfJourneys());
        }
    }

    private int getPossibleMinNumberOfChanges(final Location<?> start, final Location<?> destination,
                                              final JourneyRequest journeyRequest, Duration maxInitialWait) {
        /*
         * Route change calc issue: for example media city is on the Eccles route but trams terminate at Etihad or
         * don't go on towards Eccles depending on the time of day
         * SO only use the possible min value for number of changes, no way to safely compute a max number
         */

        // TODO Closure handling??

        return routeToRouteCosts.getNumberOfChanges(start, destination, journeyRequest,
                journeyRequest.getJourneyTimeRange(maxInitialWait));
    }

    public Stream<Journey> calculateRouteWalkAtEnd(GraphTransaction txn, Location<?> start, GraphNode endOfWalk, LocationCollection destinations,
                                                   JourneyRequest journeyRequest, int numberOfChanges, Running running)
    {
        final GraphNode startNode = getLocationNodeSafe(txn, start);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        final RunningRoutesAndServices.FilterForDate routesAndServicesFilter = runningRoutesAndServices.getFor(journeyRequest);

        final Duration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(start, config);

        return getJourneyStream(txn, startNode, endOfWalk, destinations, journeyRequest, queryTimes, routesAndServicesFilter, numberOfChanges, maxInitialWait, running).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    public Stream<Journey> calculateRouteWalkAtStart(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startOfWalkNode,
                                                     Location<?> destination,
                                                     JourneyRequest journeyRequest, int numberOfChanges, Running running) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        final GraphNode endNode = getLocationNodeSafe(txn, destination);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        Duration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(stationWalks, config);

        final RunningRoutesAndServices.FilterForDate routesAndServicesFilter = runningRoutesAndServices.getFor(journeyRequest);

        final LocationCollection destinations = LocationCollectionSingleton.of(destination);

        return getJourneyStream(txn, startOfWalkNode, endNode, destinations, journeyRequest, queryTimes, routesAndServicesFilter, numberOfChanges, maxInitialWait, running).
                limit(journeyRequest.getMaxNumberOfJourneys()).
                takeWhile(finished::notDoneYet);
    }

    public Stream<Journey> calculateRouteWalkAtStartAndEnd(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startNode, GraphNode endNode,
                                                           LocationCollection destinations, JourneyRequest journeyRequest,
                                                           int numberOfChanges, Running running) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        final RunningRoutesAndServices.FilterForDate routesAndServicesFilter = runningRoutesAndServices.getFor(journeyRequest);

        final Duration maxInitialWait = TramchesterConfig.getMaxInitialWaitFor(stationWalks, config);
        return getJourneyStream(txn, startNode, endNode, destinations, journeyRequest, queryTimes, routesAndServicesFilter,
                numberOfChanges, maxInitialWait, running).
                takeWhile(finished::notDoneYet);
    }

    protected abstract TramNetworkTraverserFactory getTraverserFactoryForGrids(StationsBoxSimpleGrid destinationBox,
                                                                               List<StationsBoxSimpleGrid> startingBoxes);

    protected abstract TramNetworkTraverserFactory getTraverserFactory(LocationCollection destinations, Set<GraphNodeId> destinationNodeIds);

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

            TransportStage<?, ?> walkingStage = journey.getStages().getFirst();

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
