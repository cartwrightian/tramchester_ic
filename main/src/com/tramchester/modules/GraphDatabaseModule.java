package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.inMemory.FindLinkedStationsInMemory;
import com.tramchester.graph.core.inMemory.GraphDatabaseInMemory;
import com.tramchester.graph.core.inMemory.NumberOfNodesAndRelationshipsRepositoryInMemory;
import com.tramchester.graph.core.neo4j.FindLinkedStationsNeo4J;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.core.neo4j.RouteCostCalculatorNeo4J;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.graph.search.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.search.TramRouteCalculator;
import com.tramchester.graph.search.neo4j.NumberOfNodesAndRelationshipsRepositoryNeo4J;
import com.tramchester.graph.search.neo4j.RouteCalculatorNeo4J;

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
            bind(NumberOfNodesAndRelationshipsRepository.class).to(NumberOfNodesAndRelationshipsRepositoryInMemory.class);
        } else {
            bind(GraphDatabase.class).to(GraphDatabaseNeo4J.class);
            bind(FindLinkedStations.class).to(FindLinkedStationsNeo4J.class);
            bind(RouteCostCalculator.class).to(RouteCostCalculatorNeo4J.class);
            bind(TramRouteCalculator.class).to(RouteCalculatorNeo4J.class);
            bind(NumberOfNodesAndRelationshipsRepository.class).to(NumberOfNodesAndRelationshipsRepositoryNeo4J.class);
        }
    }


}
