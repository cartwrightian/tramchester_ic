package com.tramchester.graph.search.inMemory;

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
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.*;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;

@LazySingleton
public class RouteCalculatorForBoxesInMemory extends RouteCalculatorForBoxes {

    @Inject
    public RouteCalculatorForBoxesInMemory(TramchesterConfig config, TransportData transportData, GraphDatabase graphDatabaseService, PathToStages pathToStages, ProvidesNow providesNow, MapPathToLocations mapPathToLocations, BetweenRoutesCostRepository routeToRouteCosts, ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndService, RouteCostCalculator routeCostCalculator, StationAvailabilityRepository stationAvailabilityRepository, CreateJourneyDiagnostics failedJourneyDiagnostics, NumberOfNodesAndRelationshipsRepository countsNodes, InterchangeRepository interchangeRepository, CacheMetrics cacheMetrics, CreateQueryTimes createQueryTimes) {
        super(config, transportData, graphDatabaseService, pathToStages, providesNow, mapPathToLocations, routeToRouteCosts, closedStationsRepository, runningRoutesAndService, routeCostCalculator, stationAvailabilityRepository, failedJourneyDiagnostics, countsNodes, interchangeRepository, cacheMetrics, createQueryTimes);
    }

    @Override
    protected TramNetworkTraverserFactory getTraverserFactoryForGrids(StationsBoxSimpleGrid destinationBox, List<StationsBoxSimpleGrid> startingBoxes) {

        final LocationSet<Station> destinations = destinationBox.getStations();
        final Set<GraphNodeId> destinationNodeIds = getDestinationNodeIds(destinations);

        return txn -> new TramNetworkTraverserInMemory(config, destinationNodeIds, txn, destinations);
    }
}
