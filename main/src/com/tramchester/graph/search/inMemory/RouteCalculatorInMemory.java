package com.tramchester.graph.search.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.diagnostics.CreateJourneyDiagnostics;
import com.tramchester.graph.search.neo4j.selectors.BranchSelectorFactory;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.*;
import jakarta.inject.Inject;

import java.util.Set;

@LazySingleton
public class RouteCalculatorInMemory extends RouteCalculatorSupport implements TramRouteCalculator {

    @Inject
    public RouteCalculatorInMemory(PathToStages pathToStages, GraphDatabase graphDatabaseService, ProvidesNow providesNow,
                                   MapPathToLocations mapPathToLocations, StationRepository stationRepository, TramchesterConfig config,
                                   BetweenRoutesCostRepository routeToRouteCosts, CreateJourneyDiagnostics failedJourneyDiagnostics,
                                   StationAvailabilityRepository stationAvailabilityRepository, NumberOfNodesAndRelationshipsRepository countsNodes,
                                   ClosedStationsRepository closedStationsRepository, CacheMetrics cacheMetrics, BranchSelectorFactory branchSelectorFactory, InterchangeRepository interchangeRepository, CreateQueryTimes createQueryTimes, RunningRoutesAndServices runningRoutesAndServices) {
        super(pathToStages, graphDatabaseService, providesNow, mapPathToLocations, stationRepository, config, routeToRouteCosts,
                failedJourneyDiagnostics, stationAvailabilityRepository, countsNodes, closedStationsRepository,
                cacheMetrics, branchSelectorFactory, interchangeRepository, createQueryTimes, runningRoutesAndServices);
    }

    @Override
    protected TramNetworkTraverserFactory getTraverserFactory(LocationCollection destinations, Set<GraphNodeId> destinationNodeIds) {
        return txn -> new TramNetworkTraverserInMemory(config, destinationNodeIds, txn, destinations);
    }
}
