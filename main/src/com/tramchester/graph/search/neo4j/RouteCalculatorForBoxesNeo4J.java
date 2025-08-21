package com.tramchester.graph.search.neo4j;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.graph.RouteCostCalculator;
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
public class RouteCalculatorForBoxesNeo4J extends RouteCalculatorForBoxes {

    private final BranchSelectorFactory branchSelectorFactory;

    @Inject
    public RouteCalculatorForBoxesNeo4J(TramchesterConfig config, TransportData transportData, GraphDatabase graphDatabaseService, PathToStages pathToStages, ProvidesNow providesNow, MapPathToLocations mapPathToLocations, BetweenRoutesCostRepository routeToRouteCosts, ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndService, RouteCostCalculator routeCostCalculator, StationAvailabilityRepository stationAvailabilityRepository, CreateJourneyDiagnostics failedJourneyDiagnostics, NumberOfNodesAndRelationshipsRepository countsNodes, InterchangeRepository interchangeRepository, BranchSelectorFactory branchSelectorFactory, CacheMetrics cacheMetrics, CreateQueryTimes createQueryTimes, BranchSelectorFactory branchSelectorFactory1) {
        super(config, transportData, graphDatabaseService, pathToStages, providesNow, mapPathToLocations, routeToRouteCosts, closedStationsRepository, runningRoutesAndService, routeCostCalculator, stationAvailabilityRepository, failedJourneyDiagnostics, countsNodes, interchangeRepository, cacheMetrics, createQueryTimes);
        this.branchSelectorFactory = branchSelectorFactory;
    }

    @Override
    protected TramNetworkTraverserFactory getTraverserFactoryForGrids(StationsBoxSimpleGrid destinationBox, List<StationsBoxSimpleGrid> startingBoxes) {

        final BranchOrderingPolicy selector = branchSelectorFactory.getForGrid(destinationBox, startingBoxes);

        final LocationSet<Station> destinations = destinationBox.getStations();
        final Set<GraphNodeId> destinationNodeIds = getDestinationNodeIds(destinations);

        return new TramNetworkTraverserFactoryNeo4J(super.config, true,
                selector, destinations, destinationNodeIds);
    }
}
