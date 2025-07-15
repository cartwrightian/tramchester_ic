package com.tramchester.integration.graph.databaseManagement;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDatabaseNeo4JTest {

    private static UnitTestOfGraphConfig config;
    private static ComponentContainer componentContainer;

    @BeforeAll
    static void beforeEachTest() throws IOException {
        config = new UnitTestOfGraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void afterEachTest() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @Test
    void shouldStartDatabase() {
        GraphDatabaseNeo4J graphDatabase = componentContainer.get(GraphDatabaseNeo4J.class);

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(5000));
        graphDatabase.stop();

        // TODO EXPAND ME
    }

}
