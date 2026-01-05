package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.inMemory.GraphCore;
import com.tramchester.graph.core.inMemory.GraphNodeInMemory;
import com.tramchester.graph.core.inMemory.NodeIdInMemory;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


// SEE ALSO TramGraphBulderTest with TestEnv graph DB set to in memory......
// Not duplicating those tests here

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

    @Test
    void shouldHaveConsistentRelationshipResults() {

        GraphCore graph = componentContainer.get(GraphCore.class);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        GraphTransaction txn = graphDatabase.beginTx();

        for(GraphDirection direction : GraphDirection.values()) {
            for (GraphLabel label : GraphLabel.values()) {
                final List<GraphNodeInMemory> nodes = graph.findNodes(label).toList();

                for (GraphNodeInMemory node : nodes) {

                    final NodeIdInMemory nodeId = node.getId();

                    long numFromNode = node.getRelationships(txn, direction, EnumSet.allOf(TransportRelationshipTypes.class)).count();
                    long numViaGraph = graph.getRelationshipsFor(nodeId, direction).count();

                    assertEquals(numFromNode, numViaGraph);
                }
            }
        }
    }

    @Disabled("performance")
    @RepeatedTest(value = 3)
    void shouldHaveConsistentBuildsOfTheDB() {

        GraphCore fromFirst = componentContainer.get(GraphCore.class);

        GuiceContainerDependencies secondContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        secondContainer.initialise();
        StagedTransportGraphBuilder builder = secondContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();

        GraphCore graphB =  secondContainer.get(GraphCore.class);

        assertEquals(fromFirst, graphB);

        secondContainer.close();


    }



}
