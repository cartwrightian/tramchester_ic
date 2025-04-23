package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.ImmutableNodeCache;
import com.tramchester.graph.databaseManagement.GraphDatabaseLifecycleManager;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TestGroupType;
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
    private GraphDBConfig dbConfig;
    private DataSourceRepository dataSourceRepository;
    private Path dbPath;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {

        dataSourceConfigs = new ArrayList<>();

        TramchesterConfig config = new IntegrationTestConfig(TestGroupType.unit) {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return dataSourceConfigs;
            }
        };

        dbConfig = config.getGraphDBConfig();
        dbPath = dbConfig.getDbPath();


        lifecycleManager = createMock(GraphDatabaseLifecycleManager.class);
        graphDatabaseService = createMock(GraphDatabaseService.class);
        dataSourceRepository = createMock(DataSourceRepository.class);

        ImmutableNodeCache nodeCache = createMock(ImmutableNodeCache.class);
        graphDatabase = new GraphDatabase(config, dataSourceRepository, lifecycleManager, nodeCache);


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

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository, dbPath, false)).
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

        EasyMock.expect(lifecycleManager.startDatabase(dataSourceRepository,  dbPath, true)).
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
