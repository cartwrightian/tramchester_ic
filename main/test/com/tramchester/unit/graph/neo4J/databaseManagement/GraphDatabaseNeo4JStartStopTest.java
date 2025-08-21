package com.tramchester.unit.graph.neo4J.databaseManagement;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.core.neo4j.*;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.GraphDBType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDatabaseNeo4JStartStopTest extends EasyMockSupport {

 private List<GTFSSourceConfig> dataSourceConfigs;
    private GraphDatabaseNeo4J graphDatabase;
    private GraphDatabaseLifecycleManager lifecycleManager;
    private GraphDatabaseService graphDatabaseService;
    private GraphDBConfig dbConfig;
    private DataSourceRepository dataSourceRepository;
    private Path dbPath;
    private GraphTransactionFactoryFactory graphTransactionFactoryFactory;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {

        dataSourceConfigs = new ArrayList<>();

        TramchesterConfig config = new IntegrationTestConfig(TestGroupType.unit, GraphDBType.Neo4J) {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return dataSourceConfigs;
            }
        };

        dbConfig = config.getGraphDBConfig();
        dbPath = dbConfig.getDbPath();

        GraphReferenceMapper graphReferenceMapper = new GraphReferenceMapper();

        lifecycleManager = createMock(GraphDatabaseLifecycleManager.class);
        graphDatabaseService = createMock(GraphDatabaseService.class);
        dataSourceRepository = createMock(DataSourceRepository.class);

        graphTransactionFactoryFactory = createMock(GraphTransactionFactoryFactory.class);

        graphDatabase = new GraphDatabaseNeo4J(config, dataSourceRepository, lifecycleManager,
                graphReferenceMapper, graphTransactionFactoryFactory);

        Files.deleteIfExists(dbConfig.getDbPath());
        Files.createDirectories(dbPath.getParent());
        Files.createFile(dbPath);
    }

    @AfterEach
    public void afterEachTestRuns() throws IOException {
        Files.deleteIfExists(dbConfig.getDbPath());
    }

    @Test
    void shouldStartLifeCycleManagerCleanExistingFile() {

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository, dbPath, true)).
                andReturn(graphDatabaseService);
        GraphTransactionFactory graphTransaction = createMock(GraphTransactionFactory.class);
        EasyMock.expect(graphTransactionFactoryFactory.create(graphDatabaseService, dbConfig)).andReturn(graphTransaction);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(true);

        graphTransaction.close();
        EasyMock.expectLastCall();

        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertTrue(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();

    }

    @Test
    void shouldStartLifeCycleNoFile() throws IOException {
        Files.delete(dbConfig.getDbPath());

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository, dbPath, false)).
                andReturn(graphDatabaseService);
        GraphTransactionFactory graphTransaction = createMock(GraphTransactionFactory.class);
        EasyMock.expect(graphTransactionFactoryFactory.create(graphDatabaseService, dbConfig)).andReturn(graphTransaction);

        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(true);

        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        graphTransaction.close();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertTrue(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();

    }

    @Test
    void shouldStartLifeCycleManagerNotCleanExistingFile() {

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository,  dbPath, true)).
                andReturn(graphDatabaseService);
        GraphTransactionFactory graphTransaction = createMock(GraphTransactionFactory.class);

        EasyMock.expect(graphTransactionFactoryFactory.create(graphDatabaseService, dbConfig)).andReturn(graphTransaction);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(false);

        graphTransaction.close();
        EasyMock.expectLastCall();

        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertFalse(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();
    }

}
