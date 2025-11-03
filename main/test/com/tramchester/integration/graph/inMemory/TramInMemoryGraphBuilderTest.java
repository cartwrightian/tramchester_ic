package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.core.inMemory.Graph;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


// SEE ALSO TramGraphBulderTest with TestEnv graph DB set to in memory......
// Not duplicating those tests here

@Disabled("To check for inconsistency in in mem graph rebuild, but seems ok")
class TramInMemoryGraphBuilderTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig(GraphDBType.InMemory);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();
    }

    @AfterEach
    void afterEachTestRuns() {
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @RepeatedTest(value = 3)
    void shouldHaveConsistentBuildsOfTheDB() {

        Graph fromFirst = componentContainer.get(Graph.class);

        GuiceContainerDependencies secondContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        secondContainer.initialise();
        StagedTransportGraphBuilder builder = secondContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();

        Graph graphB =  secondContainer.get(Graph.class);

        assertEquals(fromFirst, graphB);

        secondContainer.close();


    }



}
