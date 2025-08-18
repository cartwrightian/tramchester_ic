package com.tramchester.graph.search.neo4j;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.diagnostics.CreateJourneyDiagnostics;
import com.tramchester.graph.search.neo4j.selectors.BranchSelectorFactory;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.*;
import jakarta.inject.Inject;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculatorNeo4J extends RouteCalculatorSupport implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorNeo4J.class);
    private final CreateQueryTimes createQueryTimes;
    private final RunningRoutesAndServices runningRoutesAndServices;

    // TODO Refactoring here, way too messy and confusing constructor

    @Inject
    public RouteCalculatorNeo4J(TransportData transportData, PathToStages pathToStages,
                                TramchesterConfig config, CreateQueryTimes createQueryTimes,
                                GraphDatabase graphDatabaseService,
                                ProvidesNow providesNow, MapPathToLocations mapPathToLocations,
                                BetweenRoutesCostRepository routeToRouteCosts,
                                ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndServices,
                                CacheMetrics cacheMetrics, BranchSelectorFactory branchSelectorFactory,
                                StationAvailabilityRepository stationAvailabilityRepository, CreateJourneyDiagnostics failedJourneyDiagnostics,
                                NumberOfNodesAndRelationshipsRepository countsNodes, InterchangeRepository interchangeRepository) {
        super(pathToStages, graphDatabaseService,
                providesNow, mapPathToLocations, transportData, config, routeToRouteCosts,
                failedJourneyDiagnostics, stationAvailabilityRepository, true, countsNodes,
                closedStationsRepository, cacheMetrics, branchSelectorFactory, interchangeRepository);
        this.createQueryTimes = createQueryTimes;
        this.runningRoutesAndServices = runningRoutesAndServices;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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


    @Override
    protected TramNetworkTraverserFactoryNeo4J getTraverserFactory(LocationCollection destinations, Set<GraphNodeId> destinationNodeIds) {
        // share selector across queries, to allow caching of station to station distances
        final BranchOrderingPolicy selector = branchSelectorFactory.getFor(destinations);
        return new TramNetworkTraverserFactoryNeo4J(config, true, selector, destinations, destinationNodeIds);
    }

}

