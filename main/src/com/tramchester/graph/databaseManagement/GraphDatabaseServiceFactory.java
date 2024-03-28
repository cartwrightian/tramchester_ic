package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.metrics.Timing;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.connectors.HttpsConnector;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.DatabaseEventContext;
import org.neo4j.graphdb.event.DatabaseEventListener;
import org.neo4j.io.ByteUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

@LazySingleton
public class GraphDatabaseServiceFactory implements DatabaseEventListener {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseServiceFactory.class);

    private static final int STARTUP_TIMEOUT = 1000;
    private final GraphDBConfig dbConfig;
    private final GraphFilterActive graphFilterActive;
    private final String dbName;
    private final Path graphFile;
    private DatabaseManagementService managementServiceImpl;

    @Inject
    public GraphDatabaseServiceFactory(TramchesterConfig config, GraphFilterActive graphFilterActive) {
        dbConfig = config.getGraphDBConfig();
        this.graphFilterActive = graphFilterActive;
        dbName = DEFAULT_DATABASE_NAME; // must be this for neo4j community edition default DB
        graphFile = dbConfig.getDbPath().toAbsolutePath();
    }

    @PostConstruct
    private void start() {
        logger.info("start");
        if (graphFilterActive.isActive() && Files.exists(graphFile)) {
            String msg = "Filtering is active but graph db already present at " + graphFile.toAbsolutePath();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        logger.info("DBName : '"+ dbName + "' Path:'" + graphFile.toString() +"'");
        //createManagementService(); - slow, only do when needed
        logger.info("started");
    }

    @PreDestroy
    private void stop() {
        logger.info("Stopping");
        if (managementServiceImpl!=null) {
            logger.warn("DatabaseManagementService was not previously shutdown");
            managementServiceImpl.unregisterDatabaseEventListener(this);
            managementServiceImpl.shutdown();
        } else {
            logger.info("DatabaseManagementService was previously shutdown");
        }
        logger.info("stopped");
    }

    private void createManagementService() {

        if (dbConfig.enableDiagnostics()) {
            logger.warn("DB diagnostics enabled, neo4j bolt is enabled");
        }

        try (Timing ignored = new Timing(logger, "DatabaseManagementService build")) {
            final long neo4jPagecacheMemory = ByteUnit.parse(dbConfig.getNeo4jPagecacheMemory());
            final long memoryTransactionGlobalMaxSize = ByteUnit.parse(dbConfig.getMemoryTransactionGlobalMaxSize());
            managementServiceImpl = new DatabaseManagementServiceBuilder( graphFile ).

            // TODO need to bring this back somehow, memory usage has crept up without it

//                    setConfig(GraphDatabaseSettings.track_query_allocation, false).
//                    setConfig(GraphDatabaseSettings.store_internal_log_level, Level.WARN ).

                    // see https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/#heap-sizing
                    setConfig(GraphDatabaseSettings.pagecache_memory, neo4jPagecacheMemory).

                    setConfig(GraphDatabaseSettings.memory_transaction_global_max_size, memoryTransactionGlobalMaxSize).

                    // txn logs, no need to save beyond current one
                    setConfig(GraphDatabaseSettings.keep_logical_logs, "false").

                    // perf test, made little difference
                    //setConfig(GraphDatabaseSettings.pagecache_buffered_flush_enabled, true).

                    setConfig(GraphDatabaseSettings.debug_log_enabled, false).

                    // operating in embedded mode
                    setConfig(HttpConnector.enabled, false).
                    setConfig(HttpsConnector.enabled, false).
                    setConfig(BoltConnector.enabled, dbConfig.enableDiagnostics()).

                    build();
        }

        managementServiceImpl.registerDatabaseEventListener(this);
    }

    private DatabaseManagementService getManagementService() {
        if (managementServiceImpl==null) {
            logger.info("Starting DatabaseManagementService");
            createManagementService();
        } else {
            logger.info("DatabaseManagementService was already running");
        }
        return managementServiceImpl;
    }

    public void shutdownDatabase() {
        logger.info("Shutdown");
        // NOTE: cannot shutdown using name in community edition
        // managementService.shutdownDatabase(dbName);
        // ALSO: have to recreate managementService after shutdown otherwise DB does not start
        if (managementServiceImpl==null) {
            logger.error("Attempt to shutdown when DatabaseManagementService already stopped");
        } else {
            logger.info("Stopping DatabaseManagementService");
            managementServiceImpl.unregisterDatabaseEventListener(this);
            managementServiceImpl.shutdown();
            managementServiceImpl = null;
        }

        logger.info("Stopped");
    }

    public GraphDatabaseService create() {
        // for community edition name must be DEFAULT_DATABASE_NAME
        logger.info("Start for " + dbName + " at " + graphFile.toString());

        // create DB service for our DB
        DatabaseManagementService managementService = getManagementService();
        GraphDatabaseService graphDatabaseService = managementService.database(dbName);

        managementService.listDatabases().forEach(databaseName -> {
            logger.info("Database from managementService: " + databaseName);
        });

        logger.info("Wait for GraphDatabaseService available");
        int retries = 100;
        // NOTE: DB can just silently fail to start if updated net4j versions, so cleanGraph in this scenario
        while (!graphDatabaseService.isAvailable(STARTUP_TIMEOUT) && retries>0) {
            logger.error("GraphDatabaseService is not available (neo4j updated? dbClean needed?), name: " + dbName +
                    " Path: " + graphFile.toAbsolutePath() + " check " + retries);
            retries--;
        }

        if (!graphDatabaseService.isAvailable(STARTUP_TIMEOUT)) {
            throw new RuntimeException("Could not start " + dbConfig.getDbPath());
        }

        logger.info("GraphDatabaseService is available");
        return graphDatabaseService;
    }

    @Override
    public void databaseStart(DatabaseEventContext eventContext) {
        logger.info("database event: start " + eventContext.getDatabaseName());
    }

    @Override
    public void databaseShutdown(DatabaseEventContext eventContext) {
        logger.warn("database event: shutdown " + eventContext.getDatabaseName());
    }

    @Override
    public void databasePanic(DatabaseEventContext eventContext) {
        logger.error("database event: panic " + eventContext.getDatabaseName());
    }

    @Override
    public void databaseCreate(DatabaseEventContext eventContext) {
        logger.info("database event: create " + eventContext.getDatabaseName());
    }

    @Override
    public void databaseDrop(DatabaseEventContext eventContext) {
        logger.info("database event: drop " + eventContext.getDatabaseName());
    }

}
