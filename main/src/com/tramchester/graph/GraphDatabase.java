package com.tramchester.graph;

import com.google.inject.ImplementedBy;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.TimedTransaction;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@ImplementedBy(GraphDatabaseNeo4J.class)
public interface GraphDatabase {
    boolean isCleanDB();

    ImmutableGraphTransaction beginTx();

    ImmutableGraphTransaction beginTx(Duration timeout);

    ImmutableGraphTransaction beginTx(int timeout, TimeUnit timeUnit);

    MutableGraphTransaction beginTxMutable(int timeout, TimeUnit timeUnit);

    MutableGraphTransaction beginTxMutable();

    MutableGraphTransaction beginTxMutable(Duration timeout);

    TimedTransaction beginTimedTxMutable(Logger logger, String text);

    boolean isAvailable(long timeoutMillis);

    void waitForIndexes();

    void createIndexes();
}
