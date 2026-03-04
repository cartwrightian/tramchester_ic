package com.tramchester.unit.graph.inMemory;

import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.inMemory.GraphCore;
import com.tramchester.graph.core.inMemory.GraphIdFactory;
import com.tramchester.graph.core.inMemory.GraphInMemoryServiceManager;
import com.tramchester.graph.core.inMemory.persist.GraphPersistence;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphInMemoryServiceManagerTest extends EasyMockSupport {


    private GraphInMemoryServiceManager serviceManager;
    private GraphPersistence graphPersistence;
    private GraphIdFactory graphIdFactory;
    private GraphDatabaseStoredVersions storedVersions;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesNow = new ProvidesLocalNow();
        graphIdFactory = new GraphIdFactory();

        AppConfiguration config = TestEnv.GET();
        storedVersions = createMock(GraphDatabaseStoredVersions.class);
        graphPersistence = createMock(GraphPersistence.class);

        serviceManager = new GraphInMemoryServiceManager(graphIdFactory, storedVersions, providesNow, config, graphPersistence);

    }

    @Test
    void shouldStartWhenFileDoesNotExist() {
        TramTransportDataForTestFactory factory = new TramTransportDataForTestFactory(new ProvidesLocalNow());
        DataSourceRepository dataSourceRepository = factory.getTestData();

        Path path = Path.of("noSuchPath");

        replayAll();
        serviceManager.startDatabase(dataSourceRepository, path, false);
        assertTrue(serviceManager.isCleanDB());
        verifyAll();
    }

    @Test
    void shouldStartWhenFileDoesExistAndIsUptodate() {
        TramTransportDataForTestFactory factory = new TramTransportDataForTestFactory(new ProvidesLocalNow());
        DataSourceRepository dataSourceRepository = factory.getTestData();

        Path path = Path.of("testData/graph/");

        EasyMock.expect(graphPersistence.filesExistIn(path)).andReturn(true);
        GraphCore graphCore = new GraphCore(graphIdFactory, false);
        EasyMock.expect(graphPersistence.loadDBFrom(path, graphIdFactory)).andReturn(graphCore);

        EasyMock.expect(storedVersions.upToDate(EasyMock.eq(dataSourceRepository), EasyMock.anyObject(GraphTransaction.class))).andReturn(true);

        replayAll();
        serviceManager.startDatabase(dataSourceRepository, path, true);
        assertFalse(serviceManager.isCleanDB());
        verifyAll();
    }

    @Test
    void shouldStartWhenFileDoesExistAndNotUptodate() {
        TramTransportDataForTestFactory factory = new TramTransportDataForTestFactory(new ProvidesLocalNow());
        DataSourceRepository dataSourceRepository = factory.getTestData();

        Path path = Path.of("testData/graph/");

        EasyMock.expect(graphPersistence.filesExistIn(path)).andReturn(true);
        GraphCore graphCore = new GraphCore(graphIdFactory, false);
        EasyMock.expect(graphPersistence.loadDBFrom(path, graphIdFactory)).andReturn(graphCore);

        EasyMock.expect(storedVersions.upToDate(EasyMock.eq(dataSourceRepository), EasyMock.anyObject(GraphTransaction.class))).andReturn(false);

        replayAll();
        serviceManager.startDatabase(dataSourceRepository, path, true);
        assertTrue(serviceManager.isCleanDB());
        verifyAll();
    }
}
