package com.tramchester.graph;

import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.neo4j.TimedTransaction;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface GraphDatabase {
    boolean isCleanDB();

    GraphTransaction beginTx();

    GraphTransaction beginTx(Duration timeout);

    GraphTransaction beginTx(int timeout, TimeUnit timeUnit);

    MutableGraphTransaction beginTxMutable(int timeout, TimeUnit timeUnit);

    MutableGraphTransaction beginTxMutable();

    MutableGraphTransaction beginTxMutable(Duration timeout);

    TimedTransaction beginTimedTxMutable(Logger logger, String text);

    boolean isAvailable(long timeoutMillis);

    void waitForIndexes();

    void createIndexes();
}
