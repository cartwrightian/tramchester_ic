package com.tramchester.graph;

import org.neo4j.graphdb.GraphDatabaseService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/***
 * NOTE: not under normal lifecycle control as is used during DB startup which happens before main graph DB is created
 */
public class GraphTransactionFactory {
    private final GraphDatabaseService databaseService;

    public GraphTransactionFactory(GraphDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public GraphTransaction begin() {
        return new GraphTransaction(databaseService.beginTx());
    }

    public GraphTransaction begin(int timeout, TimeUnit timeUnit) {
        return new GraphTransaction(databaseService.beginTx(timeout, timeUnit));
    }

    public GraphTransaction begin(Duration timeout) {
        return new GraphTransaction(databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS));
    }
}
