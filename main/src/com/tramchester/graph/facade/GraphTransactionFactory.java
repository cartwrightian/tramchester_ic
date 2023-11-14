package com.tramchester.graph.facade;

import org.neo4j.graphdb.GraphDatabaseService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/***
 * NOTE: not under normal lifecycle control as is used during DB startup which happens before main graph DB is created
 */
public class GraphTransactionFactory {
    private final GraphDatabaseService databaseService;
    private final GraphIdFactory graphIdFactory;

    public GraphTransactionFactory(GraphDatabaseService databaseService, GraphIdFactory graphIdFactory) {
        this.databaseService = databaseService;
        this.graphIdFactory = graphIdFactory;
    }

    public MutableGraphTransaction begin() {
        return new MutableGraphTransaction(databaseService.beginTx(), graphIdFactory);
    }

    public MutableGraphTransaction begin(int timeout, TimeUnit timeUnit) {
        return new MutableGraphTransaction(databaseService.beginTx(timeout, timeUnit), graphIdFactory);
    }

    public MutableGraphTransaction begin(Duration timeout) {
        return new MutableGraphTransaction(databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS), graphIdFactory);
    }
}
