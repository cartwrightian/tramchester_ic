package com.tramchester.graph;

import com.google.inject.ImplementedBy;
import com.tramchester.graph.facade.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.facade.MutableGraphTransactionNeo4J;
import com.tramchester.graph.facade.TimedTransaction;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@ImplementedBy(GraphDatabaseNeo4J.class)
public interface GraphDatabase {
    boolean isCleanDB();

    ImmutableGraphTransactionNeo4J beginTx();

    ImmutableGraphTransactionNeo4J beginTx(Duration timeout);

    ImmutableGraphTransactionNeo4J beginTx(int timeout, TimeUnit timeUnit);

    MutableGraphTransactionNeo4J beginTxMutable(int timeout, TimeUnit timeUnit);

    MutableGraphTransactionNeo4J beginTxMutable();

    MutableGraphTransactionNeo4J beginTxMutable(Duration timeout);

    TimedTransaction beginTimedTxMutable(Logger logger, String text);

    boolean isAvailable(long timeoutMillis);

    void waitForIndexes();

    void createIndexes();
}
