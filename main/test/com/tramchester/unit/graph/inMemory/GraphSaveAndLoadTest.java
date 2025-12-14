package com.tramchester.unit.graph.inMemory;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.integration.graph.inMemory.RouteCalculatorInMemoryTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.core.GraphDirection.Incoming;
import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.TransportRelationshipTypes.TO_SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("WIP - memory issues via gradle")
public class GraphSaveAndLoadTest {
    private static final Path GRAPH_FILENAME = Path.of("graph_test.json");
    private static GuiceContainerDependencies componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig(GraphDBType.InMemory);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        if (Files.exists(GRAPH_FILENAME)) {
            FileUtils.delete(GRAPH_FILENAME.toFile());
        }

    }

    @Test
    void shouldSerialiseToFileWithoutError() {
        SaveGraph saveGraph = componentContainer.get(SaveGraph.class);

        saveGraph.save(GRAPH_FILENAME);
        assertTrue(Files.exists(GRAPH_FILENAME));

        Graph graph = componentContainer.get(Graph.class);
        NodesAndEdges expected = graph.getCore();

        NodesAndEdges result = SaveGraph.load(GRAPH_FILENAME);
        assertEquals(expected, result);
    }

    @Test
    void shouldBuildInMemoryFromSerialisedForm() {
        SaveGraph saveGraph = componentContainer.get(SaveGraph.class);

        saveGraph.save(GRAPH_FILENAME);
        assertTrue(Files.exists(GRAPH_FILENAME));

        Graph result = SaveGraph.loadDBFrom(GRAPH_FILENAME);
        Graph expected = componentContainer.get(Graph.class);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        assertEquals(expected.getCore(), result.getCore());

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(expected, result, txn);
        }

        assertEquals(expected, result);

        expected.stop();
        result.stop();
    }

    @Test
    void shouldCheckCompare() {

        Graph loadedGraph = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);
        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(loadedGraph, componentContainer);

        graphDatabase.start();

        // A-> A
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(loadedGraph, loadedGraph, txn);
        }
    }

    @Test
    void shouldNotHaveMissingTripsOnAnyServiceRelations() {

        SaveGraph saveGraph = componentContainer.get(SaveGraph.class);
        saveGraph.save(GRAPH_FILENAME);

        Graph loadedGraph = SaveGraph.loadDBFrom(GRAPH_FILENAME);
        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(loadedGraph, componentContainer);

        graphDatabase.start();

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            Stream<GraphNode> nodes = txn.findNodes(GraphLabel.ROUTE_STATION);

            List<GraphNode> haveMissingTrips = nodes.
                    filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            anyMatch(rel -> rel.getTripIds().isEmpty())).toList();

            List<GraphNode> allMissingTripIds = haveMissingTrips.stream().
                    filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            allMatch(rel -> rel.getTripIds().isEmpty())).toList();

            assertEquals(0, allMissingTripIds.size());

            assertEquals(0, haveMissingTrips.size());

        }
    }

    @Test
    void shouldLoadConsistently() {

        Graph graphA = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);
        Graph graphB = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);

        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(graphA, componentContainer);
        graphDatabase.start();

        Graph.same(graphA, graphB);

        // A-> A
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(graphA, graphB, txn);
        }
    }


    @Disabled("WIP - need to reproduce the failure")
    @Test
    void shouldCompareGoodAndBad() {

        Graph graphA = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);
        Graph graphB = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_FAIL);

        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(graphA, componentContainer);
        graphDatabase.start();

        Graph.same(graphA, graphB);
        checkSame(graphA, graphB, graphDatabase.beginTx());
    }

    private static void checkSame(Graph expected, Graph result, GraphTransaction txnForExpected) {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            assertEquals(expected.getNumberOf(relationshipType), result.getNumberOf(relationshipType), "wrong for " + relationshipType);
        }

        for(GraphLabel label : GraphLabel.values()) {
            final List<GraphNodeInMemory> expectedNodes = expected.findNodes(label).toList();
            final List<GraphNodeInMemory> resultNodes = result.findNodes(label).toList();
            assertEquals(expectedNodes, resultNodes, "mismatch for " + label);

            for (GraphNodeInMemory expectedNode : expectedNodes) {
                final GraphNodeInMemory resultNode = result.getNode(expectedNode.getId());
                assertEquals(expectedNode.getId(), resultNode.getId());
                assertEquals(expectedNode.getProperties(), resultNode.getProperties());

                checkRelationships(txnForExpected, GraphDirection.Outgoing, expectedNode, result);
                checkRelationships(txnForExpected, Incoming, expectedNode, result);
            }
        }

        assertTrue(Graph.same(expected, result));
    }

    private static void checkRelationships(GraphTransaction txn, GraphDirection direction,
                                           GraphNodeInMemory expected, Graph result) {

        final NodeIdInMemory nodeId = expected.getId();

        long numForExpected = expected.getRelationships(txn, direction, EnumSet.allOf(TransportRelationshipTypes.class)).count();
        long resultCount = result.getRelationshipsFor(nodeId, direction).count();
        assertEquals(numForExpected, resultCount, "for " + expected.getAllProperties());

        for(TransportRelationshipTypes transportRelationshipType : TransportRelationshipTypes.values()) {

            final Set<GraphRelationship> expectedRelationships = expected.
                    getRelationships(txn, direction, transportRelationshipType).
                    collect(Collectors.toSet());

            //assertFalse(expectedRelationships.isEmpty(), "empty for " + expected + " " + expected.getAllProperties());

            final Set<GraphRelationship> resultRelationships = result.
                    getRelationshipsFor(nodeId, direction).
                    filter(relat -> relat.isType(transportRelationshipType)).
                    map(relat -> (GraphRelationship) relat).
                    collect(Collectors.toSet());

            //assertFalse(resultRelationships.isEmpty());

            Set<GraphRelationship> diff = SetUtils.disjunction(expectedRelationships, resultRelationships);

            assertTrue(diff.isEmpty(), "diff:" + diff +  " failed for " + transportRelationshipType + " expected:" +
                    expectedRelationships + " vs result:" + resultRelationships);
        }
    }


    public static @NotNull GraphDatabaseInMemory CreateGraphDatabaseInMemory(Graph graphA, GuiceContainerDependencies container) {
        ProvidesNow providesNow = container.get(ProvidesNow.class);
        TransactionManager transactionManager = new TransactionManager(providesNow, graphA);
        return new GraphDatabaseInMemory(transactionManager);
    }

}
