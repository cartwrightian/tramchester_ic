package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.facade.GraphIdFactory;
import com.tramchester.graph.facade.GraphTransactionFactory;
import com.tramchester.graph.databaseManagement.GraphDatabaseLifecycleManager;
import com.tramchester.graph.databaseManagement.GraphDatabaseServiceFactory;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;

public class GraphDatabaseLifecycleManagerTest extends EasyMockSupport {

    private GraphDatabaseLifecycleManager graphDatabaseLifecycleManager;
    private GraphDatabaseStoredVersions storedVersions;
    private GraphDatabaseServiceFactory serviceFactory;
    private GraphDatabaseService graphDatabaseService;

    private final Path dbFile = Path.of("someFilename");
    private DataSourceRepository dataSourceRepos;

    @BeforeEach
    public void onceBeforeEachTestRuns() {

        TramchesterConfig config = TestEnv.GET();

        graphDatabaseService = createMock(GraphDatabaseService.class);
        serviceFactory = createMock(GraphDatabaseServiceFactory.class);
        storedVersions = createMock(GraphDatabaseStoredVersions.class);
        dataSourceRepos = createMock(DataSourceRepository.class);
        GraphIdFactory graphIdFactory = createMock(GraphIdFactory.class);
        graphDatabaseLifecycleManager = new GraphDatabaseLifecycleManager(config, serviceFactory, storedVersions);
    }

    @Test
    public void startImmediatelyIfExistsAndStoredVersionsOK() {

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);
        EasyMock.expect(storedVersions.upToDate(EasyMock.isA(GraphTransactionFactory.class), EasyMock.eq(dataSourceRepos))).andReturn(true);

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceRepos, dbFile, true);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }

    @Test
    public void startImmediatelyIfNotExists() {

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceRepos, dbFile, false);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }

    @Test
    public void startAndThenStopIfExistsAndStoredVersionsStale() {

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);

        EasyMock.expect(storedVersions.upToDate(EasyMock.isA(GraphTransactionFactory.class),
                EasyMock.eq(dataSourceRepos))).andReturn(false);
        serviceFactory.shutdownDatabase();
        EasyMock.expectLastCall();

        // wait for shutdown
        EasyMock.expect(graphDatabaseService.isAvailable(200L)).andReturn(true);
        EasyMock.expect(graphDatabaseService.isAvailable(200L)).andReturn(false);

        // restart
        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceRepos, dbFile, true);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }
}
