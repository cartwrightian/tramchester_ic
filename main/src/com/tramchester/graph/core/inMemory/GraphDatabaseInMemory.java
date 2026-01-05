package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.MutableGraphTransaction;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@LazySingleton
public class GraphDatabaseInMemory implements GraphDatabase {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseInMemory.class);

    private final TransactionManager transactionManager;
    static Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Inject
    public GraphDatabaseInMemory(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @PostConstruct
    public void start() {
        logger.warn("EXPERIMENTAL");
        if (started.get()) {
            throw new RuntimeException("Already started");
        }
        logger.info("starting");
        started.set(true);
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        if (started.get()) {
            logger.info("stopped");
        } else {
            logger.warn("Not running");
        }
    }

    void guardForNotStarted() {
        if (!started.get()) {
            throw new RuntimeException("Not started");
        }
    }

    @Override
    public boolean isCleanDB() {
        return true;
    }

    // begin immutable

    @Override
    public GraphTransaction beginTx() {
        return beginTx(DEFAULT_TIMEOUT);
    }

    @Override
    public GraphTransaction beginTx(final Duration timeout) {
        return beginTxInMemory(timeout, true);
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
        return beginTxInMemory(timeout, false);
    }

    @Override
    public MutableGraphTransaction beginTimedTxMutable(Logger logger, String text) {
        guardForNotStarted();
        return transactionManager.createTimedTransaction(logger, text, false);
    }

    private MutableGraphTransaction beginTxInMemory(final Duration timeout, boolean immutable) {
        guardForNotStarted();
        return transactionManager.createTransaction(timeout, immutable);
    }

    @Override
    public boolean isAvailable(long timeoutMillis) {
        return started.get();
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
