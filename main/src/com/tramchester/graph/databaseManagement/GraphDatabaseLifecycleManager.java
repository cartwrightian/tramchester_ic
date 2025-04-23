package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.caches.ImmutableNodeCache;
import com.tramchester.graph.facade.GraphTransactionFactory;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.DataSourceRepository;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

@LazySingleton
public class GraphDatabaseLifecycleManager {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseLifecycleManager.class);

    private static final int SHUTDOWN_TIMEOUT = 200;

    private final GraphDatabaseServiceFactory serviceFactory;
    private final GraphDatabaseStoredVersions storedVersions;
    private final TramchesterConfig configuration;

    private GraphDBConfig graphDBConfig;
    private boolean cleanDB;
    private final ImmutableNodeCache nodeCache;

    @Inject
    public GraphDatabaseLifecycleManager(TramchesterConfig configuration, GraphDatabaseServiceFactory serviceFactory,
                                         GraphDatabaseStoredVersions storedVersions, ImmutableNodeCache nodeCache) {
        this.configuration = configuration;
        this.serviceFactory = serviceFactory;
        this.storedVersions = storedVersions;
        this.nodeCache = nodeCache;
    }

    public GraphDatabaseService startDatabase(final DataSourceRepository dataSourceRepository, final Path graphFile, final boolean fileExists) {
        logger.info("Create or load graph " + graphFile);
        this.graphDBConfig = configuration.getGraphDBConfig();

        if (fileExists) {
            logger.info("Graph db file is present at " + graphFile);
        } else {
            logger.info("No db file found at " + graphFile);
        }

        cleanDB = !fileExists;
        GraphDatabaseService databaseService = serviceFactory.create();
        final GraphTransactionFactory transactionFactory = new GraphTransactionFactory(databaseService, nodeCache, graphDBConfig.enableDiagnostics());

        if (fileExists && !storedVersions.upToDate(transactionFactory, dataSourceRepository)) {
            logger.warn("Graph is out of date, rebuild needed");
            cleanDB = true;
            serviceFactory.shutdownDatabase();
            waitForShutdown(databaseService);
            try {
                logger.warn("Out of date, Deleting " + graphFile.toAbsolutePath());
                FileUtils.deleteDirectory(graphFile.toFile());
            } catch (IOException e) {
                String message = "Cannot delete out of date graph DB";
                logger.error(message,e);
                throw new RuntimeException(message,e);
            }
            databaseService = serviceFactory.create();
        }

        transactionFactory.close();

        logger.info("graph db started at:" + graphFile);

        return databaseService;
    }

    private void waitForShutdown(GraphDatabaseService databaseService) {
        int count = 5000 / SHUTDOWN_TIMEOUT; // wait 5 seconds
        while (databaseService.isAvailable(SHUTDOWN_TIMEOUT)) {
            logger.info("Waiting for graph shutdown");
            count--;
            if (count==0) {
                throw new RuntimeException("Cannot shutdown database");
            }
        }
    }

    public void stopDatabase() {
        try (Timing ignored = new Timing(logger, "DatabaseManagementService stopping")) {
            serviceFactory.shutdownDatabase();
        } catch (Exception exceptionInClose) {
            logger.error("DatabaseManagementService Exception during close down " + graphDBConfig.getDbPath(), exceptionInClose);
        }
        logger.info("DatabaseManagementService Stopped");
    }

    public boolean isCleanDB() {
        return cleanDB;
    }

}
