package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
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

@Disabled("Memory issues running from gradle")
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

        GraphCore graph = componentContainer.get(GraphCore.class);
        NodesAndEdges expected = graph.getNodesAndEdges();

        NodesAndEdges result = SaveGraph.load(GRAPH_FILENAME);
        assertEquals(expected, result);
    }

    @Test
    void shouldSaveAndLoadAndCompare() {
        SaveGraph saveGraph = componentContainer.get(SaveGraph.class);

        saveGraph.save(GRAPH_FILENAME);
        assertTrue(Files.exists(GRAPH_FILENAME));

        GraphCore result = SaveGraph.loadDBFrom(GRAPH_FILENAME);
        GraphCore expected = componentContainer.get(GraphCore.class);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        assertEquals(expected.getNodesAndEdges(), result.getNodesAndEdges());

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(expected, result, txn);
        }

        assertEquals(expected, result);

        expected.stop();
        result.stop();
    }

    @Disabled("WIP - needs data in sync to work")
    @Test
    void shouldCheckCompare() {

        GraphCore loadedGraph = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);
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

        GraphCore loadedGraph = SaveGraph.loadDBFrom(GRAPH_FILENAME);
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
    void shouldLoadConsistently() throws InterruptedException {
        SaveGraph saveGraph = componentContainer.get(SaveGraph.class);
        saveGraph.save(GRAPH_FILENAME);

        // race condition?? Flush is being call but file is empty?
//        final File file = GRAPH_FILENAME.toFile();
//        int count = 10;
//        while (file.length()==0 && count-->0) {
//            Thread.sleep(20);
//        }
//        assertNotEquals(0, file.length());

        GraphCore graphA = SaveGraph.loadDBFrom(GRAPH_FILENAME);
        GraphCore graphB = SaveGraph.loadDBFrom(GRAPH_FILENAME);

        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(graphA, componentContainer);
        graphDatabase.start();

        GraphCore.same(graphA, graphB);

        // A-> A
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(graphA, graphB, txn);
        }
    }

    @Disabled("WIP - need to reproduce the failure")
    @Test
    void shouldCompareGoodAndBad() {

        GraphCore graphA = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);
        GraphCore graphB = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_FAIL);

        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(graphA, componentContainer);
        graphDatabase.start();

        GraphCore.same(graphA, graphB);
        checkSame(graphA, graphB, graphDatabase.beginTx());
    }

    private static void checkSame(GraphCore expected, GraphCore result, GraphTransaction txnForExpected) {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            assertEquals(expected.getNumberOf(relationshipType), result.getNumberOf(relationshipType), "wrong for " + relationshipType);
        }

        for(GraphLabel label : GraphLabel.values()) {
            final List<GraphNodeInMemory> expectedNodes = expected.findNodesMutable(label).toList();
            final List<GraphNodeInMemory> resultNodes = result.findNodesMutable(label).toList();
            assertEquals(expectedNodes, resultNodes, "mismatch for " + label);

            for (GraphNodeInMemory expectedNode : expectedNodes) {
                final GraphNodeInMemory resultNode = result.getNodeMutable(expectedNode.getId());
                assertEquals(expectedNode.getId(), resultNode.getId());
                assertEquals(expectedNode.getProperties(), resultNode.getProperties());

                checkRelationships(txnForExpected, GraphDirection.Outgoing, expectedNode, result);
                checkRelationships(txnForExpected, Incoming, expectedNode, result);
            }
        }

        assertTrue(GraphCore.same(expected, result));
    }

    private static void checkRelationships(GraphTransaction txn, GraphDirection direction,
                                           GraphNodeInMemory expected, GraphCore result) {

        final NodeIdInMemory nodeId = expected.getId();

        long numForExpected = expected.getRelationships(txn, direction, EnumSet.allOf(TransportRelationshipTypes.class)).count();
        long resultCount = result.findRelationshipsImmutableFor(nodeId, direction).count();
        assertEquals(numForExpected, resultCount, "for " + expected.getAllProperties());

        for(TransportRelationshipTypes transportRelationshipType : TransportRelationshipTypes.values()) {

            final Set<GraphRelationship> expectedRelationships = expected.
                    getRelationships(txn, direction, transportRelationshipType).
                    collect(Collectors.toSet());

            final Set<GraphRelationship> resultRelationships = result.
                    findRelationshipsImmutableFor(nodeId, direction).
                    filter(relat -> relat.isType(transportRelationshipType)).
                    collect(Collectors.toSet());

            Set<GraphRelationship> diff = SetUtils.disjunction(expectedRelationships, resultRelationships);

            assertTrue(diff.isEmpty(), "diff:" + diff +  " failed for " + transportRelationshipType + " expected:" +
                    expectedRelationships + " vs result:" + resultRelationships);
        }
    }


    public static @NotNull GraphDatabaseInMemory CreateGraphDatabaseInMemory(GraphCore graphCore, GuiceContainerDependencies container) {
        ProvidesNow providesNow = container.get(ProvidesNow.class);
        GraphIdFactory idFactory = container.get(GraphIdFactory.class);
        TransactionManager transactionManager = new TransactionManager(providesNow, graphCore, idFactory);
        return new GraphDatabaseInMemory(transactionManager);
    }

}
