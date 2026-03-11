package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.inMemory.persist.GraphPersistence;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.repository.DataSourceRepository;
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
    private final GraphDatabaseStoredVersions storedVersions;
    private final ProvidesNow providesNow;
    private final TramchesterConfig config;
    private final GraphPersistence graphPersistence;

    private GraphCore graphCore;
    private TransactionManager transactionManager;
    private boolean loadedFromDisc;

    @Inject
    public GraphInMemoryServiceManager(GraphIdFactory idFactory, GraphDatabaseStoredVersions storedVersions,
                                       ProvidesNow providesNow, TramchesterConfig config, GraphPersistence graphPersistence) {
        this.idFactory = idFactory;
        this.storedVersions = storedVersions;
        this.providesNow = providesNow;
        this.config = config;
        this.graphPersistence = graphPersistence;
        graphCore = null;
    }

    @PostConstruct
    private void start() {

    }

    @PreDestroy
    private void stop() {
        if (config.getPlanningEnabled()) {
            // likely already stopped via GraphDatabaseInMemory
            stopDatabase(false, Path.of(""));
        } else {
            logger.info("Planning was not enabled");
        }
    }

    public void startDatabase(DataSourceRepository dataSourceRepository, Path dbFolderPath, boolean folderExists) {
        if (graphCore != null) {
            String message = "Already started";
            logger.error(message);
            throw new RuntimeException(message);
        }

        logger.info("Starting DB");

        final boolean filesExist = folderExists && graphPersistence.filesExistIn(dbFolderPath);

        final boolean createEmptyDB;
        if (filesExist) {
            logger.info("Load DB from " + dbFolderPath.toAbsolutePath());
            // load from disc
            loadFrom(dbFolderPath);
            loadedFromDisc = true;

            try (GraphTransaction transaction = transactionManager.createTransaction(GraphDatabaseInMemory.DEFAULT_TXN_TIMEOUT, true)) {
                createEmptyDB = !storedVersions.upToDate(dataSourceRepository, transaction);
            }
            if (createEmptyDB) {
                logger.warn("DB loaded, out of date " + dbFolderPath.toAbsolutePath());
                transactionManager.stop();
                graphCore.stop();
            } else {
                logger.info("DB loaded, up to date " + dbFolderPath.toAbsolutePath());
            }
        }  else {
            logger.info("Did not find persisted DB at " + dbFolderPath.toAbsolutePath());
            createEmptyDB = true;
        }

        if (createEmptyDB) {
            logger.warn("Creating clean DB");
            loadedFromDisc = false;
            graphCore = new GraphCore(idFactory, false);
            graphCore.start();
            transactionManager = new TransactionManager(providesNow, graphCore, idFactory);
        }

        logger.info("started");
    }

    public void stopDatabase(final boolean saveGraph, final Path dbFolderPath) {
        logger.info("Stopping DB");
        if (graphCore==null) {
            logger.warn("Already stopped");
        } else {
            if (saveGraph) {
                logger.info("Saved DB to " + dbFolderPath.toAbsolutePath());
                boolean result = graphPersistence.save(dbFolderPath, this);
                if (result) {
                    logger.info("Saved DB at " + dbFolderPath.toAbsolutePath());
                } else {
                    logger.error("Failed to save DB, check logs");
                }
            } else {
                logger.info("Save not requested");
            }
            transactionManager.stop();
            graphCore.stop();
            graphCore = null;
        }
        logger.info("Stopped DB");
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

    private void loadFrom(final Path path) {
        if (this.graphCore!=null) {
            String message = "Attempted to overwrite via load";
            logger.error(message);
            throw new RuntimeException(message);
        }

        if (!Files.exists(path)) {
            String message = "Could not find " + path.toAbsolutePath();
            logger.error(message);
            throw new RuntimeException(message);
        }

        final GraphCore core = graphPersistence.loadDBFrom(path, idFactory);
        this.graphCore = core;
        this.transactionManager = new TransactionManager(providesNow, core, idFactory);
    }

    public boolean isCleanDB() {
        return !loadedFromDisc;
    }


}
