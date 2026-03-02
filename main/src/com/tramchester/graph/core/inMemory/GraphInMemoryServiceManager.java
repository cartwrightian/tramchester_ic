package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.inMemory.persist.SaveGraph;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;

@LazySingleton
public class GraphInMemoryServiceManager {
    private static final Logger logger = LoggerFactory.getLogger(GraphInMemoryServiceManager.class);

    private final GraphIdFactory idFactory;
    private final ProvidesNow providesNow;
    private final TramchesterConfig config;

    private GraphCore graphCore;
    private TransactionManager transactionManager;

    @Inject
    public GraphInMemoryServiceManager(GraphIdFactory idFactory, ProvidesNow providesNow, TramchesterConfig config) {
        this.idFactory = idFactory;
        this.providesNow = providesNow;
        this.config = config;
        graphCore = null;
    }

    @PostConstruct
    public void start() {
        if (config.getPlanningEnabled()) {

            if (graphCore == null) {
                logger.info("Starting DB");
                graphCore = new GraphCore(idFactory, false);
                graphCore.start();
                transactionManager = new TransactionManager(providesNow, graphCore, idFactory);

                // TODO LOAD FROM DISK IF FILE PRESENT - Path from Config

                logger.info("started");
            } else {
                String message = "Already started";
                logger.error(message);
                throw new RuntimeException(message);
            }

        } else {
            logger.warn("Planning not enabled");
        }
    }

    @PreDestroy
    void stop() {
        logger.info("Stoping DB");
        if (config.getPlanningEnabled()) {
            guardNotStarted();
            transactionManager.stop();
            graphCore.stop();
            graphCore = null;
        } else {
            logger.info("Planning was not enabled");
        }
        logger.info("Stopped");
    }

    private void guardNotStarted() {
        if (graphCore==null) {
            String message = "Not started";
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    public GraphCore getGraphCore() {
        guardNotStarted();
        return graphCore;
    }

    public TransactionManager getTransactionManager() {
        guardNotStarted();
        return transactionManager;
    }

    public void loadFrom(final Path path) {
        // TODO Rework once path into config
        if (!Files.exists(path)) {
            String message = "Could not find " + path.toAbsolutePath();
            logger.error(message);
            throw new RuntimeException(message);
        }
        GraphCore core = SaveGraph.loadDBFrom(path);
        this.graphCore = core;
        this.transactionManager = new TransactionManager(providesNow, core, idFactory);
    }
}
