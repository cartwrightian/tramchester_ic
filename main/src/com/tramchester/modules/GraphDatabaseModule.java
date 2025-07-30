package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.graph.facade.GraphDatabase;
import com.tramchester.graph.facade.neo4j.GraphDatabaseNeo4J;

public class GraphDatabaseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GraphDatabase.class).to(GraphDatabaseNeo4J.class);
    }
}
