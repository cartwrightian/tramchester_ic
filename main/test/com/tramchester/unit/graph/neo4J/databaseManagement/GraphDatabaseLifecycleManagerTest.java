package com.tramchester.unit.graph.neo4J.databaseManagement;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.neo4j.*;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.config.GraphDBTestConfig;
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
    private GraphTransactionFactoryFactory graphTransactionFactoryFactory;
    private GraphDBTestConfig dbConfig;

    @BeforeEach
    public void onceBeforeEachTestRuns() {

        //TramchesterConfig config = TestEnv.GET();

        TramchesterConfig config = createMock(TramchesterConfig.class);
        dbConfig = new GraphDBTestConfig(TestGroupType.unit, TestEnv.GET());
        EasyMock.expect(config.getGraphDBConfig()).andReturn(dbConfig);

        graphDatabaseService = createMock(GraphDatabaseService.class);
        serviceFactory = createMock(GraphDatabaseServiceFactory.class);
        storedVersions = createMock(GraphDatabaseStoredVersions.class);
        dataSourceRepos = createMock(DataSourceRepository.class);

        graphTransactionFactoryFactory = createStrictMock(GraphTransactionFactoryFactory.class);

        graphDatabaseLifecycleManager = new GraphDatabaseLifecycleManager(config, serviceFactory, storedVersions,
                graphTransactionFactoryFactory);

    }

    @Test
    public void startImmediatelyIfExistsAndStoredVersionsOK() {

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);

        GraphTransactionFactory transactionFactory = createStrictMock(GraphTransactionFactory.class);

        EasyMock.expect(graphTransactionFactoryFactory.create(graphDatabaseService, dbConfig)).andReturn(transactionFactory);

        GraphTransaction graphTransaction = createStrictMock(GraphTransaction.class);

        EasyMock.expect(transactionFactory.begin(GraphDatabaseNeo4J.DEFAULT_TXN_TIMEOUT)).andReturn(graphTransaction);
        graphTransaction.close();
        EasyMock.expectLastCall();

        EasyMock.expect(storedVersions.upToDate(dataSourceRepos, graphTransaction)).andReturn(true);

        transactionFactory.close();
        EasyMock.expectLastCall();

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceRepos, dbFile, true);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }

    @Test
    public void startImmediatelyIfNotExists() {

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);
        GraphTransactionFactory graphTransactionFactory = createMock(GraphTransactionFactory.class);
        EasyMock.expect(graphTransactionFactoryFactory.create(graphDatabaseService, dbConfig)).andReturn(graphTransactionFactory);

        graphTransactionFactory.close();
        EasyMock.expectLastCall();

        replayAll();
        GraphDatabaseService result = graphDatabaseLifecycleManager.startDatabase(dataSourceRepos, dbFile, false);
        verifyAll();

        assertSame(graphDatabaseService, result);
    }

    @Test
    public void startAndThenStopIfExistsAndStoredVersionsStale() {

        EasyMock.expect(serviceFactory.create()).andReturn(graphDatabaseService);

        GraphTransactionFactory graphTransactionFactory = createMock(GraphTransactionFactory.class);
        EasyMock.expect(graphTransactionFactoryFactory.create(graphDatabaseService, dbConfig)).andReturn(graphTransactionFactory);

        GraphTransaction transaction = createMock(GraphTransaction.class);
        EasyMock.expect(graphTransactionFactory.begin(GraphDatabaseNeo4J.DEFAULT_TXN_TIMEOUT)).andReturn(transaction);
        transaction.close();
        EasyMock.expectLastCall();

        EasyMock.expect(storedVersions.upToDate(dataSourceRepos,transaction)).andReturn(false);

        graphTransactionFactory.close();
        EasyMock.expectLastCall();

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
