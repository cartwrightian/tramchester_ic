package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.MutableGraphTransaction;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class GraphDatabaseInMemory implements GraphDatabase {

    private final TransactionManager transactionManager;
    static Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

    @Inject
    public GraphDatabaseInMemory(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public boolean isCleanDB() {
        return true;
    }

    @Override
    public GraphTransaction beginTx() {
        return beginTx(DEFAULT_TIMEOUT);
    }

    @Override
    public GraphTransaction beginTx(final Duration timeout) {
        return beginTxInMemory(timeout);
    }

    private MutableGraphTransaction beginTxInMemory(final Duration timeout) {
        return transactionManager.createTransaction(timeout);
    }

    @Override
    public GraphTransaction beginTx(int timeout, TimeUnit timeUnit) {
        return beginTx(Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    @Override
    public MutableGraphTransaction beginTxMutable(int timeout, TimeUnit timeUnit) {
        return beginTxMutable(Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    @Override
    public MutableGraphTransaction beginTxMutable() {
        return beginTxMutable(DEFAULT_TIMEOUT);
    }

    @Override
    public MutableGraphTransaction beginTxMutable(Duration timeout) {
        return beginTxInMemory(timeout);
    }

    @Override
    public MutableGraphTransaction beginTimedTxMutable(Logger logger, String text) {
        return transactionManager.createTimedTransaction(logger, text);
    }

    @Override
    public boolean isAvailable(long timeoutMillis) {
        return true;
    }

    @Override
    public void waitForIndexes() {
        // no-op
    }

    @Override
    public void createIndexes() {
        // no-op
    }
}
