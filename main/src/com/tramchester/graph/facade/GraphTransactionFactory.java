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

    public GraphTransaction begin() {
        return new GraphTransaction(databaseService.beginTx(), graphIdFactory);
    }

    public GraphTransaction begin(int timeout, TimeUnit timeUnit) {
        return new GraphTransaction(databaseService.beginTx(timeout, timeUnit), graphIdFactory);
    }

    public GraphTransaction begin(Duration timeout) {
        return new GraphTransaction(databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS), graphIdFactory);
    }
}
