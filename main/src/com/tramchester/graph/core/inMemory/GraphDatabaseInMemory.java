package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.DataSourceRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class GraphDatabaseInMemory implements GraphDatabase {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseInMemory.class);

    static Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

    private final GraphInMemoryServiceManager serviceManager;
    private final TramchesterConfig config;
    private final DataSourceRepository dataSourceRepository;
    private final GraphFilterActive graphFilterActive;

    private TransactionManager transactionManager;

    @Inject
    public GraphDatabaseInMemory(final GraphInMemoryServiceManager serviceManager, final TramchesterConfig config, DataSourceRepository dataSourceRepository, GraphFilterActive graphFilterActive) {
        this.serviceManager = serviceManager;
        this.config = config;
        this.dataSourceRepository = dataSourceRepository;
        this.graphFilterActive = graphFilterActive;
        transactionManager = null;
    }

    @PostConstruct
    public void start() {
        if (config.getPlanningEnabled()) {
            if (transactionManager!=null) {
                throw new RuntimeException("Already started");
            }

            logger.info("starting");
            final GraphDBConfig graphDBConfig = config.getGraphDBConfig();
            final Path dbPath = graphDBConfig.getDbPath();
            boolean fileExists = Files.exists(dbPath);
            if (graphFilterActive.isActive() && fileExists) {
                String msg = "Graph filter is active, but file exists " + dbPath;
                logger.error(msg);
                throw new RuntimeException(msg);
            }
            serviceManager.startDatabase(dataSourceRepository, dbPath, fileExists);
            transactionManager = serviceManager.getTransactionManager();
            logger.info("started");
        } else {
            logger.warn("Planning not enabled");
        }
    }

    @PreDestroy
    public void stop() {
        final GraphDBConfig graphDBConfig = config.getGraphDBConfig();
        final Path dbPath = graphDBConfig.getDbPath();

        logger.info("Stopping");
        if (config.getPlanningEnabled()) {
            guardForNotStarted();
            if (transactionManager == null) {
                logger.warn("Not running");
            } else {
                transactionManager.stop();

                // TODO Persist boolean into GraphDBConfig
                boolean savedDB = !graphFilterActive.isActive();
                serviceManager.stopDatabase(savedDB, dbPath);
                transactionManager = null;
                logger.info("stopping");
            }
        } else {
            logger.info("Planning not enabled");
        }
    }

    void guardForNotStarted() {
        if (transactionManager==null) {
            throw new RuntimeException("Not started");
        }
    }

    @Override
    public boolean isCleanDB() {
        return serviceManager.isCleanDB();
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
        //final TransactionManager transactionManager = serviceManager.getTransactionManager();
        return transactionManager.createTimedTransaction(logger, text, false);
    }

    private MutableGraphTransaction beginTxInMemory(final Duration timeout, boolean immutable) {
        guardForNotStarted();
        //final TransactionManager transactionManager = serviceManager.getTransactionManager();
        return transactionManager.createTransaction(timeout, immutable);
    }

    @Override
    public boolean isAvailable(long timeoutMillis) {
        return transactionManager!=null;
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
