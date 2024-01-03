package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.databaseManagement.GraphDatabaseLifecycleManager;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.repository.DataSourceRepository;
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

class GraphDatabaseStartStopTest extends EasyMockSupport {

 private List<GTFSSourceConfig> dataSourceConfigs;
    private GraphDatabase graphDatabase;
    private GraphDatabaseLifecycleManager lifecycleManager;
    private GraphDatabaseService graphDatabaseService;
    private GraphDBTestConfig dbConfig;
    private DataSourceRepository dataSourceRepository;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        String dbName = "graphDbTest.db";

        dbConfig = new GraphDBTestConfig("graphDatabaseTest", dbName);
        TramchesterConfig config = new IntegrationTestConfig(dbConfig) {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return dataSourceConfigs;
            }
        };

        dataSourceConfigs = new ArrayList<>();

        lifecycleManager = createMock(GraphDatabaseLifecycleManager.class);
        graphDatabaseService = createMock(GraphDatabaseService.class);
        dataSourceRepository = createMock(DataSourceRepository.class);

//        GraphIdFactory graphIdFactory = createMock(GraphIdFactory.class);

        graphDatabase = new GraphDatabase(config, dataSourceRepository, lifecycleManager);

        final Path dbPath = dbConfig.getDbPath();
        Files.createDirectories(dbPath.getParent());
        Files.createFile(dbPath);
    }

    @AfterEach
    public void afterEachTestRuns() throws IOException {
        Files.deleteIfExists(dbConfig.getDbPath());
    }

    @Test
    void shouldStartLifeCycleManagerCleanExistingFile() {

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository, dbConfig.getDbPath(), true)).
                andReturn(graphDatabaseService);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(true);
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

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository, dbConfig.getDbPath(), false)).
                andReturn(graphDatabaseService);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(true);
        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertTrue(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();

    }

    @Test
    void shouldStartLifeCycleManagerNotCleanExistingFile() {

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository,  dbConfig.getDbPath(), true)).
                andReturn(graphDatabaseService);
        EasyMock.expect(lifecycleManager.isCleanDB()).andReturn(false);
        lifecycleManager.stopDatabase();
        EasyMock.expectLastCall();

        replayAll();
        graphDatabase.start();
        assertFalse(graphDatabase.isCleanDB());
        graphDatabase.stop();
        verifyAll();
    }

}
