package com.tramchester.graph.core;

import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface GraphDatabase {
    Duration DEFAULT_TXN_TIMEOUT = Duration.ofMinutes(5);

    boolean isCleanDB();

    boolean isInMemory();

    GraphTransaction beginTx();

    GraphTransaction beginTx(Duration timeout);

    GraphTransaction beginTx(int timeout, TimeUnit timeUnit);

    MutableGraphTransaction beginTxMutable(int timeout, TimeUnit timeUnit);

    MutableGraphTransaction beginTxMutable();

    MutableGraphTransaction beginTxMutable(Duration timeout);

    MutableGraphTransaction beginTimedTxMutable(Logger logger, String text);

    boolean isAvailable(long timeoutMillis);

    void waitForIndexes();

    void createIndexes();
}
