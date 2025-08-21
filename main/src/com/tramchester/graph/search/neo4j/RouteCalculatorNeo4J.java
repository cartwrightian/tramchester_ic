package com.tramchester.graph.search.neo4j;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.diagnostics.CreateJourneyDiagnostics;
import com.tramchester.graph.search.neo4j.selectors.BranchSelectorFactory;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.*;
import jakarta.inject.Inject;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;

import java.util.List;
import java.util.Set;

@LazySingleton
public class RouteCalculatorNeo4J extends RouteCalculatorSupport implements TramRouteCalculator {
    private final BranchSelectorFactory branchSelectorFactory;

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
                failedJourneyDiagnostics, stationAvailabilityRepository, countsNodes,
                closedStationsRepository, cacheMetrics, interchangeRepository, createQueryTimes, runningRoutesAndServices);
        this.branchSelectorFactory = branchSelectorFactory;
    }


    @Override
    protected TramNetworkTraverserFactory getTraverserFactoryForGrids(StationsBoxSimpleGrid destinationBox, List<StationsBoxSimpleGrid> startingBoxes) {
        throw new RuntimeException("Not implemented for normal calculations");
    }

    @Override
    protected TramNetworkTraverserFactoryNeo4J getTraverserFactory(LocationCollection destinations, Set<GraphNodeId> destinationNodeIds) {
        // share selector across queries, to allow caching of station to station distances
        final BranchOrderingPolicy selector = branchSelectorFactory.getFor(destinations);
        return new TramNetworkTraverserFactoryNeo4J(config, true, selector, destinations, destinationNodeIds);
    }

}

