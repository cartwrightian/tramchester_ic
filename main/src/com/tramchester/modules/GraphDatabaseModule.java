package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.inMemory.FindLinkedStationsInMemory;
import com.tramchester.graph.core.inMemory.GraphDatabaseInMemory;
import com.tramchester.graph.core.inMemory.NumberOfNodesAndRelationshipsRepositoryInMemory;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.graph.search.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.search.RouteCalculatorForBoxes;
import com.tramchester.graph.search.TramRouteCalculator;
import com.tramchester.graph.search.inMemory.RouteCalculatorForBoxesInMemory;
import com.tramchester.graph.search.inMemory.RouteCalculatorInMemory;
import com.tramchester.graph.search.inMemory.RouteCostCalculatorInMemory;


public class GraphDatabaseModule extends AbstractModule {
    private final TramchesterConfig config;

    public GraphDatabaseModule(TramchesterConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        if (config.getInMemoryGraph()) {
            bind(GraphDatabase.class).to(GraphDatabaseInMemory.class);
            bind(FindLinkedStations.class).to(FindLinkedStationsInMemory.class);
            bind(RouteCostCalculator.class).to(RouteCostCalculatorInMemory.class);
            bind(TramRouteCalculator.class).to(RouteCalculatorInMemory.class);
            bind(NumberOfNodesAndRelationshipsRepository.class).to(NumberOfNodesAndRelationshipsRepositoryInMemory.class);
            bind(RouteCalculatorForBoxes.class).to(RouteCalculatorForBoxesInMemory.class);
        } else {
           throw new RuntimeException("No longer supported");
        }
    }


}
