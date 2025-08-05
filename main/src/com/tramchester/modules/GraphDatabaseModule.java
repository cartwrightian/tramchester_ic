package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.neo4j.FindLinkedStationsNeo4J;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.core.neo4j.RouteCostCalculatorNeo4J;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.graph.search.TramRouteCalculator;
import com.tramchester.graph.search.neo4j.RouteCalculatorNeo4J;

public class GraphDatabaseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GraphDatabase.class).to(GraphDatabaseNeo4J.class);
        bind(FindLinkedStations.class).to(FindLinkedStationsNeo4J.class);
        bind(RouteCostCalculator.class).to(RouteCostCalculatorNeo4J.class);
        bind(TramRouteCalculator.class).to(RouteCalculatorNeo4J.class);
    }


}
