package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.caches.SharedRelationshipCache;
import com.tramchester.graph.databaseManagement.GraphDatabaseLifecycleManager;
import com.tramchester.graph.facade.neo4j.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.DataSourceRepository;
import jakarta.inject.Inject;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.DatabaseEventContext;
import org.neo4j.graphdb.event.DatabaseEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@LazySingleton
public class GraphDatabaseNeo4J implements DatabaseEventListener, GraphDatabase {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseNeo4J.class);
    public static final Duration DEFAULT_TXN_TIMEOUT = Duration.ofMinutes(5);

    private final DataSourceRepository dataSourceRepository;
    private final GraphDBConfig graphDBConfig;
    private final GraphDatabaseLifecycleManager lifecycleManager;
    private final TramchesterConfig tramchesterConfig;

    private final AtomicBoolean indexesCreated;
    private final AtomicBoolean indexesOnline;

    private GraphTransactionFactory graphTransactionFactory;
    private GraphDatabaseService databaseService;
    private final SharedNodeCache nodeCache;
    private final SharedRelationshipCache relationshipCache;

    @Inject
    public GraphDatabaseNeo4J(TramchesterConfig configuration, DataSourceRepository dataSourceRepository,
                              GraphDatabaseLifecycleManager lifecycleManager, SharedNodeCache nodeCache,
                              SharedRelationshipCache relationshipCache) {
        this.dataSourceRepository = dataSourceRepository;
        this.tramchesterConfig = configuration;
        this.graphDBConfig = configuration.getGraphDBConfig();
        this.lifecycleManager = lifecycleManager;
        this.nodeCache = nodeCache;
        this.relationshipCache = relationshipCache;
        indexesOnline = new AtomicBoolean(false);
        indexesCreated = new AtomicBoolean(false);
    }

    @PostConstruct
    public void start() {
        if (tramchesterConfig.getPlanningEnabled()) {
            logger.info("start");
            final Path dbPath = graphDBConfig.getDbPath();
            boolean fileExists = Files.exists(dbPath);
            databaseService = lifecycleManager.startDatabase(dataSourceRepository, dbPath, fileExists);
            graphTransactionFactory = new GraphTransactionFactory(databaseService, nodeCache, relationshipCache,
                    graphDBConfig.enableDiagnostics());
            logger.info("graph db started ");
        } else {
            logger.warn("Planning is disabled, not starting the graph database");
        }
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        if (databaseService!=null) {
            graphTransactionFactory.close();
            lifecycleManager.stopDatabase();
            databaseService = null;
        } else {
            logger.warn("Already stopped");
        }
        logger.info("stopped");
    }

    @Override
    public boolean isCleanDB() {
        return lifecycleManager.isCleanDB();
    }

    // immutable transactions

    @Override
    public ImmutableGraphTransactionNeo4J beginTx() {
        return beginTx(DEFAULT_TXN_TIMEOUT);
    }

    @Override
    public ImmutableGraphTransactionNeo4J beginTx(final Duration timeout) {
        return graphTransactionFactory.begin(timeout);
    }

    @Override
    public ImmutableGraphTransactionNeo4J beginTx(int timeout, TimeUnit timeUnit) {
        return beginTx(Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    // mutable transactions

    @Override
    public MutableGraphTransactionNeo4J beginTxMutable(int timeout, TimeUnit timeUnit) {
        return beginTxMutable(Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    @Override
    public MutableGraphTransactionNeo4J beginTxMutable() {
        return graphTransactionFactory.beginMutable(DEFAULT_TXN_TIMEOUT);
    }

    @Override
    public MutableGraphTransactionNeo4J beginTxMutable(final Duration timeout) {
        return graphTransactionFactory.beginMutable(timeout);
    }

    @Override
    public TimedTransaction beginTimedTxMutable(final Logger logger, final String text) {
        return graphTransactionFactory.beginTimedMutable(logger, text, DEFAULT_TXN_TIMEOUT);
    }

    /////////////

    public void createIndexes() {


        try (TimedTransaction tx = beginTimedTxMutable(logger, "Create DB Constraints & indexes"))
        {

            if (indexesCreated.get()) {
                logger.warn("Indexes already created");
            } else {
                final DBSchema schema = tx.schema();

                schema.createIndex(GraphLabel.STATION, GraphPropertyKey.STATION_ID);
                schema.createIndex(GraphLabel.ROUTE_STATION, GraphPropertyKey.ROUTE_STATION_ID);
                schema.createIndex(GraphLabel.ROUTE_STATION, GraphPropertyKey.STATION_ID);
                schema.createIndex(GraphLabel.ROUTE_STATION, GraphPropertyKey.ROUTE_ID);
                schema.createIndex(GraphLabel.PLATFORM, GraphPropertyKey.PLATFORM_ID);

                indexesCreated.set(true);
            }

            tx.commit();
        }
    }

    public void waitForIndexes() {
        if (!indexesCreated.get()) {
            String msg = "Indexes not created";
            logger.error(msg);
            return;
            // TODO put this back
            //throw new RuntimeException(msg);
        }

        if (indexesOnline.get()) {
            return;
        }
        if (databaseService==null) {
            throw new RuntimeException("Database service was not started");
        }
        try(MutableGraphTransactionNeo4J tx = beginTxMutable()) {
            waitForIndexesReady(tx.schema());
            indexesOnline.set(true);
        }
    }

    private void waitForIndexesReady(DBSchema schema) {
        logger.info("Wait for indexs online");
        schema.waitForIndexes();
    }

    @Override
    public boolean isAvailable(long timeoutMillis) {
        if (databaseService == null) {
            logger.error("Checking for DB available when not started, this is will likely be a bug");
            return false;
        }
        return databaseService.isAvailable(timeoutMillis);
    }

    public EvaluationContext createContext(final GraphTransactionNeo4J txn) {
        return txn.createEvaluationContext(databaseService);
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

    @Override
    public String toString() {
        return "GraphDatabaseNeo4J{" +
                "graphDBConfig=" + graphDBConfig +
                '}';
    }
}
