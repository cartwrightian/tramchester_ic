package com.tramchester.graph.facade;

import com.tramchester.config.GraphDBConfig;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/***
 * NOTE: not under normal lifecycle control as is used during DB startup which happens before main graph DB is created
 * Do not call these directly when GraphDatabase object is available
 */
public class GraphTransactionFactory {
    private final GraphDatabaseService databaseService;
    private final GraphDBConfig graphDBConfig;

    public GraphTransactionFactory(GraphDatabaseService databaseService, GraphDBConfig graphDBConfig) {
        this.databaseService = databaseService;
        this.graphDBConfig = graphDBConfig;
    }

    public MutableGraphTransaction beginMutable(final Duration timeout) {
        // graph id factory scoped to transaction level to avoid memory usages issues
        final GraphIdFactory graphIdFactory = new GraphIdFactory(graphDBConfig);
        return new MutableGraphTransaction(databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS), graphIdFactory);
    }

    public ImmutableGraphTransaction begin(final Duration timeout) {
        final MutableGraphTransaction contained = beginMutable(timeout);
        return new ImmutableGraphTransaction(contained);
    }
}
