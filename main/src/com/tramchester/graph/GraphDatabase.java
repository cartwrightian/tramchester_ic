package com.tramchester.graph;

import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.TimedTransaction;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
