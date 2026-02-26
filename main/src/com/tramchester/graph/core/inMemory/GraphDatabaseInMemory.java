package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
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

    static Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final GraphInMemoryServiceManager serviceManager;
    private final TramchesterConfig config;
    private boolean clean;

    @Inject
    public GraphDatabaseInMemory(final GraphInMemoryServiceManager serviceManager, TramchesterConfig config) {
        this.serviceManager = serviceManager;
        this.config = config;
    }

    @PostConstruct
    public void start() {
        if (config.getPlanningEnabled()) {
            if (started.get()) {
                throw new RuntimeException("Already started");
            }
            logger.info("starting");
            started.set(true);
            clean = true;
            logger.info("started");
        } else {
            logger.warn("Planning not enabled");
        }
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        if (config.getPlanningEnabled()) {
            guardForNotStarted();
            if (started.get()) {
                logger.info("stopped");
            } else {
                logger.warn("Not running");
            }
        } else {
            logger.info("Planning not enabled");
        }
    }

    void guardForNotStarted() {
        if (!started.get()) {
            throw new RuntimeException("Not started");
        }
    }

    @Override
    public boolean isCleanDB() {
        return clean;
    }

    @Override
    public boolean isInMemory() {
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
        final TransactionManager transactionManager = serviceManager.getTransactionManager();
        return transactionManager.createTimedTransaction(logger, text, false);
    }

    private MutableGraphTransaction beginTxInMemory(final Duration timeout, boolean immutable) {
        guardForNotStarted();
        final TransactionManager transactionManager = serviceManager.getTransactionManager();
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
