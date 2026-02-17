package com.tramchester.integration.graph.compare;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.inMemory.GraphDatabaseInMemory;
import com.tramchester.graph.core.inMemory.NumberOfNodesAndRelationshipsRepositoryInMemory;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.neo4j.NumberOfNodesAndRelationshipsRepositoryNeo4J;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.Neo4JTest;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Neo4JTest
public class CompareNeo4JWithInMemoryTest {

    private static GuiceContainerDependencies componentContainerNeo4J;

    private GuiceContainerDependencies componentContainerInMemory;
    private static TramchesterConfig config;

    private NumberOfNodesAndRelationshipsRepositoryInMemory inMemoryCounts;
    private NumberOfNodesAndRelationshipsRepositoryNeo4J neo4JCounts;
    private GraphTransaction txnInMem;
    private GraphTransaction txnNeo4J;


    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig(GraphDBType.InMemory);
        TramchesterConfig configNeo4J = new IntegrationTramTestConfig(GraphDBType.Neo4J);

        componentContainerNeo4J = new ComponentsBuilder().create(configNeo4J, TestEnv.NoopRegisterMetrics());
        componentContainerNeo4J.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        componentContainerInMemory = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainerInMemory.initialise();

        inMemoryCounts = componentContainerInMemory.get(NumberOfNodesAndRelationshipsRepositoryInMemory.class);
        neo4JCounts = componentContainerNeo4J.get(NumberOfNodesAndRelationshipsRepositoryNeo4J.class);

        GraphDatabaseInMemory dbInMemory = componentContainerInMemory.get(GraphDatabaseInMemory.class);
        txnInMem = dbInMemory.beginTx();

        GraphDatabaseNeo4J dbNeo4J = componentContainerNeo4J.get(GraphDatabaseNeo4J.class);
        txnNeo4J = dbNeo4J.beginTx();
    }

    @AfterEach
    void onceAfterEachTest() {
        txnInMem.close();
        txnNeo4J.close();
        componentContainerInMemory.close();
    }

    @AfterAll
    static void onceAfterAll() {
        componentContainerNeo4J.close();
    }

    @Test
    void shouldHaveSameCountsForNodes() {
        List<GraphLabel> labels = new ArrayList<>(Arrays.asList(GraphLabel.values()));
        for (int i = 0; i < 24; i++) {
            labels.remove(GraphLabel.getHourLabel(i));
        }
        for(GraphLabel label : labels) {
            long inMem = inMemoryCounts.numberOf(label);
            long neo4J = neo4JCounts.numberOf(label);
            assertEquals(neo4J, inMem, "Mismatch for " + label);
        }
    }

    @Test
    void shouldHaveSameCountsForRelationships() {
        long totalInMem = 0;
        long totalNeo4J = 0 ;
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            long inMem = inMemoryCounts.numberOf(relationshipType);
            long neo4J = neo4JCounts.numberOf(relationshipType);
            assertEquals(neo4J, inMem, "Mismatch for " + relationshipType);
            totalInMem += inMem;
            totalNeo4J += neo4J;
        }

        assertEquals(totalNeo4J, totalInMem);
        // todo visualvm shows 483 755 relationships - this seems too high
        //assertEquals(483755, totalInMem, "temporary, this will change");

    }

}
