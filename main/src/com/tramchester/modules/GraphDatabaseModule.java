package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;

public class GraphDatabaseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GraphDatabase.class).to(GraphDatabaseNeo4J.class);
    }
}
