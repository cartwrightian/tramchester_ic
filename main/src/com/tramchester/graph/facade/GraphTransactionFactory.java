package com.tramchester.graph.facade;

import org.neo4j.graphdb.GraphDatabaseService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/***
 * NOTE: not under normal lifecycle control as is used during DB startup which happens before main graph DB is created
 * Do not call these directly when GraphDatabase object is available
 */
public class GraphTransactionFactory {
    private final GraphDatabaseService databaseService;
    private final GraphIdFactory graphIdFactory;

    public GraphTransactionFactory(GraphDatabaseService databaseService, GraphIdFactory graphIdFactory) {
        this.databaseService = databaseService;
        this.graphIdFactory = graphIdFactory;
    }

    public MutableGraphTransaction beginMutable(Duration timeout) {
        return new MutableGraphTransaction(databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS), graphIdFactory);
    }

    public ImmutableGraphTransaction begin(Duration timeout) {
        MutableGraphTransaction contained = beginMutable(timeout);
        return new ImmutableGraphTransaction(contained);
    }
}
