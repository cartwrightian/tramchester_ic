package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.caches.SharedRelationshipCache;
import com.tramchester.graph.databaseManagement.GraphDatabaseLifecycleManager;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.DataSourceRepository;
import jakarta.inject.Inject;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.DatabaseEventContext;
import org.neo4j.graphdb.event.DatabaseEventListener;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class GraphDatabase implements DatabaseEventListener {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabase.class);
    public static final Duration DEFAULT_TXN_TIMEOUT = Duration.ofMinutes(5);

    private final DataSourceRepository dataSourceRepository;
    private final GraphDBConfig graphDBConfig;
    private final GraphDatabaseLifecycleManager lifecycleManager;
    private final TramchesterConfig tramchesterConfig;
    private boolean indexesOnline;

    private GraphTransactionFactory graphTransactionFactory;
    private GraphDatabaseService databaseService;
    private final SharedNodeCache nodeCache;
    private final SharedRelationshipCache relationshipCache;

    @Inject
    public GraphDatabase(TramchesterConfig configuration, DataSourceRepository dataSourceRepository,
                         GraphDatabaseLifecycleManager lifecycleManager, SharedNodeCache nodeCache, SharedRelationshipCache relationshipCache) {
        this.dataSourceRepository = dataSourceRepository;
        this.tramchesterConfig = configuration;
        this.graphDBConfig = configuration.getGraphDBConfig();
        this.lifecycleManager = lifecycleManager;
        this.nodeCache = nodeCache;
        this.relationshipCache = relationshipCache;
        indexesOnline = false;
    }

    @PostConstruct
    public void start() {
        if (tramchesterConfig.getPlanningEnabled()) {
            logger.info("start");
            final Path dbPath = graphDBConfig.getDbPath();
            boolean fileExists = Files.exists(dbPath);
            databaseService = lifecycleManager.startDatabase(dataSourceRepository, dbPath, fileExists);
            graphTransactionFactory = new GraphTransactionFactory(databaseService, nodeCache, relationshipCache, graphDBConfig.enableDiagnostics());
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

    public boolean isCleanDB() {
        return lifecycleManager.isCleanDB();
    }

    // immutable transactions

    public ImmutableGraphTransaction beginTx() {
        return beginTx(DEFAULT_TXN_TIMEOUT);
    }

    public ImmutableGraphTransaction beginTx(final Duration timeout) {
        return graphTransactionFactory.begin(timeout);
    }

    public ImmutableGraphTransaction beginTx(int timeout, TimeUnit timeUnit) {
        return beginTx(Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    // mutable transactions

    public MutableGraphTransaction beginTxMutable(int timeout, TimeUnit timeUnit) {
        return beginTxMutable(Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    public MutableGraphTransaction beginTxMutable() {
        return graphTransactionFactory.beginMutable(DEFAULT_TXN_TIMEOUT);
    }

    public MutableGraphTransaction beginTxMutable(final Duration timeout) {
        return graphTransactionFactory.beginMutable(timeout);
    }

    public TimedTransaction beginTimedTxMutable(final Logger logger, final String text) {
        return graphTransactionFactory.beginTimedMutable(logger, text, DEFAULT_TXN_TIMEOUT);
    }

    /////////////

    public void createIndexes() {

        try (TimedTransaction tx = beginTimedTxMutable(logger, "Create DB Constraints & indexes"))
        {
            final Schema schema = tx.schema();

            createUniqueIdConstraintFor(schema, GraphLabel.STATION, GraphPropertyKey.STATION_ID);

            createUniqueIdConstraintFor(schema, GraphLabel.ROUTE_STATION, GraphPropertyKey.ROUTE_STATION_ID);
            schema.indexFor(GraphLabel.ROUTE_STATION).on(GraphPropertyKey.STATION_ID.getText()).create();
            schema.indexFor(GraphLabel.ROUTE_STATION).on(GraphPropertyKey.ROUTE_ID.getText()).create();

            schema.indexFor(GraphLabel.PLATFORM).on(GraphPropertyKey.PLATFORM_ID.getText()).create();

            tx.commit();
        }
    }

    private void createUniqueIdConstraintFor(Schema schema, GraphLabel label, GraphPropertyKey property) {
        schema.indexFor(label).on(property.getText()).create();
    }

    public void waitForIndexes() {
        if (indexesOnline) {
            return;
        }
        if (databaseService==null) {
            throw new RuntimeException("Database service was not started");
        }
        try(Transaction tx = databaseService.beginTx()) {
            waitForIndexesReady(tx.schema());
            indexesOnline = true;
        }
    }

    private void waitForIndexesReady(Schema schema) {
        logger.info("Wait for indexs online");
        schema.awaitIndexesOnline(5, TimeUnit.SECONDS);

        schema.getIndexes().forEach(indexDefinition -> {
            Schema.IndexState state = schema.getIndexState(indexDefinition);
            if (indexDefinition.isNodeIndex()) {
                logger.info(String.format("Node Index %s labels %s keys %s state %s",
                        indexDefinition.getName(),
                        indexDefinition.getLabels(), indexDefinition.getPropertyKeys(), state));
            } else {
                logger.info(String.format("Non-Node Index %s keys %s state %s",
                        indexDefinition.getName(), indexDefinition.getPropertyKeys(), state));
            }
        });

        schema.getConstraints().forEach(definition -> logger.info(String.format("Constraint label %s keys %s type %s",
                definition.getLabel(), definition.getPropertyKeys(), definition.getConstraintType()
                )));
    }

    public boolean isAvailable(long timeoutMillis) {
        if (databaseService == null) {
            logger.error("Checking for DB available when not started, this is will likely be a bug");
            return false;
        }
        return databaseService.isAvailable(timeoutMillis);
    }

    public EvaluationContext createContext(GraphTransaction txn) {
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

    public String getDbPath() {
        return graphDBConfig.getDbPath().toAbsolutePath().toString();
    }

}
