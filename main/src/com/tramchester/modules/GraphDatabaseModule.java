package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;

public class GraphDatabaseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GraphDatabase.class).to(GraphDatabaseNeo4J.class);
    }
}
