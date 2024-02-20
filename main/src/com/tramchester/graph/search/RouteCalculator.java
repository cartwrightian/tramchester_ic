package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.diagnostics.CreateFailedJourneyDiagnostics;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.selectors.BranchSelectorFactory;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculator extends RouteCalculatorSupport implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);
    private final TramchesterConfig config;
    private final CreateQueryTimes createQueryTimes;
    private final ClosedStationsRepository closedStationsRepository;
    private final RunningRoutesAndServices runningRoutesAndServices;
    private final CacheMetrics cacheMetrics;
    private final BranchSelectorFactory branchSelectorFactory;

    // TODO Refactoring here, way too messy and confusing constructor

    @Inject
    public RouteCalculator(TransportData transportData, NodeContentsRepository nodeOperations, PathToStages pathToStages,
                           TramchesterConfig config, CreateQueryTimes createQueryTimes,
                           TraversalStateFactory traversalStateFactory, GraphDatabase graphDatabaseService,
                           ProvidesNow providesNow,
                           MapPathToLocations mapPathToLocations,
                           BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                           ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndServices,
                           CacheMetrics cacheMetrics, BranchSelectorFactory branchSelectorFactory,
                           StationAvailabilityRepository stationAvailabilityRepository, CreateFailedJourneyDiagnostics failedJourneyDiagnostics) {
        super(pathToStages, nodeOperations, graphDatabaseService,
                traversalStateFactory, providesNow, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz, failedJourneyDiagnostics, stationAvailabilityRepository, true);
        this.config = config;
        this.createQueryTimes = createQueryTimes;
        this.closedStationsRepository = closedStationsRepository;
        this.runningRoutesAndServices = runningRoutesAndServices;
        this.cacheMetrics = cacheMetrics;
        this.branchSelectorFactory = branchSelectorFactory;
    }

    @Override
    public Stream<Journey> calculateRoute(GraphTransaction txn, Location<?> start, Location<?> destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s (%s) --> %s (%s) for %s",
                start.getName(), start.getId(), destination.getName(), destination.getId(), journeyRequest));

        final GraphNode startNode = getLocationNodeSafe(txn, start);
        final GraphNode endNode = getLocationNodeSafe(txn, destination);

        // for walks we pass multiple destinations TODO check this is still the case
        final LocationSet destinations = LocationSet.singleton(destination);

        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        final TramDate date = journeyRequest.getDate();

        final NumberOfChanges numberOfChanges =  routeToRouteCosts.getNumberOfChanges(start, destination, journeyRequest);

        // route change calc issue, for media city to ashton line
        // not clear if existing mechanism works now routes are bi-directional
        if (journeyRequest.getMaxChanges()>numberOfChanges.getMax()) {
            if (closedStationsRepository.hasClosuresOn(date)) {
                logger.warn(format("Closures in effect today %s so over ride max changes", journeyRequest.getDate()));
                numberOfChanges.overrideMax(journeyRequest.getMaxChanges());
            }
            else {
                logger.warn(format("Computed max changes (%s) is less than requested number of changes (%s)",
                        numberOfChanges.getMax(), journeyRequest.getMaxChanges()));
                // TODO needed due to route change calc issue
                numberOfChanges.overrideMax(journeyRequest.getMaxChanges());
            }
        }

        final Duration maxInitialWait = getMaxInitialWaitFor(start, config);
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges, maxInitialWait).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(GraphTransaction txn, Location<?> start, GraphNode endOfWalk, LocationSet destinations,
                                                   JourneyRequest journeyRequest, NumberOfChanges numberOfChanges)
    {
        GraphNode startNode = getLocationNodeSafe(txn, start);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime());

        final Duration maxInitialWait = getMaxInitialWaitFor(start, config);

        return getJourneyStream(txn, startNode, endOfWalk, journeyRequest, destinations, queryTimes, numberOfChanges, maxInitialWait).
                limit(journeyRequest.getMaxNumberOfJourneys());
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startOfWalkNode, Location<?> destination,
                                                     JourneyRequest journeyRequest, NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        final GraphNode endNode = getLocationNodeSafe(txn, destination);
        final LocationSet destinations = LocationSet.singleton(destination);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        Duration maxInitialWait = getMaxInitialWaitFor(stationWalks, config);

        return getJourneyStream(txn, startOfWalkNode, endNode, journeyRequest, destinations, queryTimes, numberOfChanges, maxInitialWait).
                takeWhile(finished::notDoneYet);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStartAndEnd(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startNode, GraphNode endNode,
                                                           LocationSet destinationStations, JourneyRequest journeyRequest,
                                                           NumberOfChanges numberOfChanges) {

        final InitialWalksFinished finished = new InitialWalksFinished(journeyRequest, stationWalks);
        final List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getOriginalTime(), stationWalks);

        Duration maxInitialWait = getMaxInitialWaitFor(stationWalks, config);
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinationStations, queryTimes, numberOfChanges, maxInitialWait).
                takeWhile(finished::notDoneYet);
    }

    private Stream<Journey> getJourneyStream(final GraphTransaction txn, final GraphNode startNode, final GraphNode endNode, final JourneyRequest journeyRequest,
                                             final LocationSet destinations, final List<TramTime> queryTimes, final NumberOfChanges numberOfChanges,
                                             final Duration maxInitialWait) {

        if (numberOfChanges.getMin()==Integer.MAX_VALUE) {
            logger.error(format("Computed min number of changes is MAX_VALUE, journey %s is not possible?", journeyRequest));
            return Stream.empty();
        }

        final EnumSet<TransportMode> requestedModes = journeyRequest.getRequestedModes();

        final Set<GraphNodeId> destinationNodeIds = Collections.singleton(endNode.getId());
        final TramDate tramDate = journeyRequest.getDate();

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        final LowestCostsForDestRoutes lowestCostsForRoutes = routeToRouteCosts.getLowestCostCalculatorFor(destinations, journeyRequest);
        final Duration maxJourneyDuration = getMaxDurationFor(journeyRequest);

        final IdSet<Station> closedStations = closedStationsRepository.getFullyClosedStationsFor(tramDate).stream().
                map(ClosedStation::getStationId).collect(IdSet.idCollector());

        final TimeRange destinationsAvailable = super.getDestinationsAvailable(destinations, tramDate);
        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, runningRoutesAndServices.getFor(tramDate),
                closedStations, destinations, lowestCostsForRoutes, maxJourneyDuration, destinationsAvailable);

        // share selector across queries, to allow caching of station to station distances
        final BranchOrderingPolicy selector = branchSelectorFactory.getFor(destinations);

        logger.info("Journey Constraints: " + journeyConstraints);
        logger.info("Query times: " + queryTimes);

        final LowestCostSeen lowestCostSeen = new LowestCostSeen();

        final AtomicInteger journeyIndex = new AtomicInteger(0);

        // TODO for now stopping computation only support for Grids
        Running running = () -> true;

        final Stream<Journey> results = numChangesRange(journeyRequest, numberOfChanges).
                flatMap(numChanges -> queryTimes.stream().
                        map(queryTime -> createPathRequest(startNode, tramDate, queryTime, requestedModes, numChanges,
                                journeyConstraints, maxInitialWait, selector))).
                flatMap(pathRequest -> findShortestPath(txn, createServiceReasons(journeyRequest, pathRequest), pathRequest,
                        createPreviousVisits(), lowestCostSeen, destinations, destinationNodeIds,
                        running)).
                map(path -> createJourney(journeyRequest, path, destinations, journeyIndex, txn));

        //noinspection ResultOfMethodCallIgnored
        results.onClose(() -> {
            cacheMetrics.report();
            logger.info("Journey stream closed");
        });

        return results;
    }

    public static final class TimedPath {
        private final Path path;
        private final TramTime queryTime;
        private final int numChanges;

        public TimedPath(final Path path, final TramTime actualQueryTime, int numChanges) {
            this.path = path;
            this.queryTime = actualQueryTime;
            this.numChanges = numChanges;
        }

        public TimedPath(final Path path, final PathRequest pathRequest) {
            this(path, pathRequest.getActualQueryTime(), pathRequest.getNumChanges());
        }

        @Override
            public String toString() {
                return "TimedPath{" +
                        "path=" + path +
                        ", queryTime=" + queryTime +
                        ", numChanges=" + numChanges +
                        '}';
            }

        public Path path() {
            return path;
        }

        public TramTime queryTime() {
            return queryTime;
        }

        public int numChanges() {
            return numChanges;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (TimedPath) obj;
            return Objects.equals(this.path, that.path) &&
                    Objects.equals(this.queryTime, that.queryTime) &&
                    this.numChanges == that.numChanges;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, queryTime, numChanges);
        }

    }
}

