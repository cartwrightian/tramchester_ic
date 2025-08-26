package com.tramchester.integration.graph;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphDatabaseInMemory;
import com.tramchester.graph.core.inMemory.NumberOfNodesAndRelationshipsRepositoryInMemory;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.neo4j.NumberOfNodesAndRelationshipsRepositoryNeo4J;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompareNeo4JWithInMemoryTest {

    private static GuiceContainerDependencies componentContainerInMemory;
    private static GuiceContainerDependencies componentContainerNeo4J;

    private NumberOfNodesAndRelationshipsRepositoryInMemory inMemoryCounts;
    private NumberOfNodesAndRelationshipsRepositoryNeo4J neo4JCounts;
    private GraphDatabaseInMemory dbInMemory;
    private GraphDatabaseNeo4J dbNeo4J;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig configInMemory = new IntegrationTramTestConfig(GraphDBType.InMemory);
        TramchesterConfig configNeo4J = new IntegrationTramTestConfig(GraphDBType.Neo4J);

        componentContainerInMemory = new ComponentsBuilder().create(configInMemory, TestEnv.NoopRegisterMetrics());
        componentContainerInMemory.initialise();

        componentContainerNeo4J = new ComponentsBuilder().create(configNeo4J, TestEnv.NoopRegisterMetrics());
        componentContainerNeo4J.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        dbInMemory = componentContainerInMemory.get(GraphDatabaseInMemory.class);
        dbNeo4J = componentContainerNeo4J.get(GraphDatabaseNeo4J.class);
        inMemoryCounts = componentContainerInMemory.get(NumberOfNodesAndRelationshipsRepositoryInMemory.class);
        neo4JCounts = componentContainerNeo4J.get(NumberOfNodesAndRelationshipsRepositoryNeo4J.class);
    }

    @AfterAll
    static void onceAfterAll() {
        componentContainerInMemory.close();
        componentContainerNeo4J.close();
    }

    @Test
    void shouldHaveSameCountsForNodes() {
        for(GraphLabel label : GraphLabel.values()) {
            long inMem = inMemoryCounts.numberOf(label);
            long neo4J = neo4JCounts.numberOf(label);
            assertEquals(neo4J, inMem, "Mismatch for " + label);
        }
    }

    @Test
    void shouldHaveSameCountsForRelationships() {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            long inMem = inMemoryCounts.numberOf(relationshipType);
            long neo4J = neo4JCounts.numberOf(relationshipType);
            assertEquals(neo4J, inMem, "Mismatch for " + relationshipType);
        }
    }

    @Test
    void shouldHaveRelationshipsAtStationNodes() {
        StationRepository stationRepository = componentContainerInMemory.get(StationRepository.class);

        try (GraphTransaction txnInMem = dbInMemory.beginTx()) {
            try (GraphTransaction txnNeo4J = dbNeo4J.beginTx()) {
                for(Station station : stationRepository.getStations()) {
                    GraphNode inMemoryNode = txnInMem.findNode(station);
                    GraphNode neo4JNode = txnNeo4J.findNode(station);
                    assertEquals(neo4JNode.getAllProperties(), inMemoryNode.getAllProperties());
                    assertEquals(neo4JNode.getLabels(), inMemoryNode.getLabels());

                    checkSame(txnNeo4J, txnInMem, neo4JNode, inMemoryNode, GraphDirection.Outgoing);
                    checkSame(txnNeo4J, txnInMem, neo4JNode, inMemoryNode, GraphDirection.Incoming);
                }
            }
        }
    }

    @Test
    void shouldHaveRelationshipsAtRouteStationNodes() {
        StationRepository stationRepository = componentContainerInMemory.get(StationRepository.class);

        try (GraphTransaction txnInMem = dbInMemory.beginTx()) {
            try (GraphTransaction txnNeo4J = dbNeo4J.beginTx()) {
                for(RouteStation routeStation : stationRepository.getRouteStations()) {
                    GraphNode inMemoryNode = txnInMem.findNode(routeStation);
                    GraphNode neo4JNode = txnNeo4J.findNode(routeStation);
                    assertEquals(neo4JNode.getAllProperties(), inMemoryNode.getAllProperties());
                    assertEquals(neo4JNode.getLabels(), inMemoryNode.getLabels());

                    checkSame(txnNeo4J, txnInMem, neo4JNode, inMemoryNode, GraphDirection.Outgoing);
                    checkSame(txnNeo4J, txnInMem, neo4JNode, inMemoryNode, GraphDirection.Incoming);
                }
            }
        }
    }

    private void checkSame(GraphTransaction txnA, GraphTransaction txnB, GraphNode graphNodeA, GraphNode graphNodeB, GraphDirection direction) {
        List<GraphRelationship> relationshipsA = graphNodeA.getRelationships(txnA, direction).toList();
        List<GraphRelationship> relationshipsB = graphNodeB.getRelationships(txnB, direction).toList();

        assertEquals(relationshipsA.size(), relationshipsB.size());

        for(TransportRelationshipTypes type : TransportRelationshipTypes.values()) {
            long countA = relationshipsA.stream().filter(relationship -> relationship.isType(type)).count();
            long countB = relationshipsB.stream().filter(relationship -> relationship.isType(type)).count();
            assertEquals(countA, countB);

            if (countA==1) {
                GraphRelationship relA = relationshipsA.stream().filter(relationship -> relationship.isType(type)).toList().getFirst();
                GraphRelationship relB = relationshipsB.stream().filter(relationship -> relationship.isType(type)).toList().getFirst();

                checkProps(relA, relB);
                //assertEquals(relA.getAllProperties(), relB.getAllProperties());
            }

        }
    }

    private void checkProps(GraphEntity graphEntityA, GraphEntity graphEntityB) {
        Map<String, Object> propsA = graphEntityA.getAllProperties();
        Map<String, Object> propsB = graphEntityB.getAllProperties();

        assertEquals(propsA.size(), propsB.size());

        for(String key : propsA.keySet()) {
            assertTrue(propsB.containsKey(key));
            assertEquals(propsA.get(key), propsB.get(key), "mismatch on " + key + " for " + graphEntityA + " and " + graphEntityB);
        }

    }
}
