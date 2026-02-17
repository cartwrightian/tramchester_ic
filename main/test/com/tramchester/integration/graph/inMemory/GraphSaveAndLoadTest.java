package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.core.inMemory.persist.SaveGraph;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.collections4.SetUtils;
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
import static com.tramchester.graph.reference.TransportRelationshipTypes.TRAM_GOES_TO;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("Memory issues running from gradle")
public class GraphSaveAndLoadTest {
    private static final Path GRAPH_FILENAME = Path.of("graph_test.json");
    private static GuiceContainerDependencies componentContainer;
    private SaveGraph saveGraph;

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
        saveGraph = componentContainer.get(SaveGraph.class);

        Files.deleteIfExists(GRAPH_FILENAME);
    }

    @AfterEach
    void onceAfterEachTestRuns() throws IOException {
        Files.deleteIfExists(GRAPH_FILENAME);
    }

    @Test
    void shouldSerialiseToFileWithoutError() {
        saveGraph.save(GRAPH_FILENAME);
        assertTrue(Files.exists(GRAPH_FILENAME));

        GraphCore graph = componentContainer.get(GraphCore.class);
        NodesAndEdges expected = graph.getNodesAndEdges();

        NodesAndEdges result = SaveGraph.load(GRAPH_FILENAME);
        assertEquals(expected, result);
    }

    @Test
    void shouldSanityCheckComparison() {
        GraphCore core = componentContainer.get(GraphCore.class);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(core, core, txn);
            assertTrue(GraphCore.same(core, core));
        }
    }

    @Test
    void shouldSaveAndLoadAndCompare() {
        GraphCore expected = componentContainer.get(GraphCore.class);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        saveGraph.save(GRAPH_FILENAME);
        assertTrue(Files.exists(GRAPH_FILENAME));

        GraphCore result = SaveGraph.loadDBFrom(GRAPH_FILENAME);

        assertEquals(expected.getNodesAndEdges(), result.getNodesAndEdges());

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            assertNotEquals(0, txn.numberOf(TRAM_GOES_TO), "sanity check on counters failed");

            checkSame(expected, result, txn);
        }

        //expected.stop();
        result.stop();
    }


    @Test
    void shouldNotHaveMissingTripsOnAnyServiceRelations() {

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
    void shouldLoadConsistently() {
        saveGraph.save(GRAPH_FILENAME);

        GraphCore graphA = SaveGraph.loadDBFrom(GRAPH_FILENAME);
        GraphCore graphB = SaveGraph.loadDBFrom(GRAPH_FILENAME);

        GraphDatabaseInMemory graphDatabaseInMemory = CreateGraphDatabaseInMemory(graphA, componentContainer);
        graphDatabaseInMemory.start();

        GraphCore.same(graphA, graphB);

        try (GraphTransaction txn = graphDatabaseInMemory.beginTx()) {
            checkSame(graphA, graphB, txn);
        }
    }

    private static void checkSame(GraphCore expected, GraphCore result, GraphTransaction txnForExpected) {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            assertEquals(expected.getNumberOf(relationshipType), result.getNumberOf(relationshipType), "wrong for " + relationshipType);
        }

        for(GraphLabel label : GraphLabel.values()) {
            final Set<GraphNodeInMemory> expectedNodes = expected.findNodesMutable(label).collect(Collectors.toSet());
            final Set<GraphNodeInMemory> resultNodes = result.findNodesMutable(label).collect(Collectors.toSet());
            SetUtils.SetView<GraphNodeInMemory> comparison = SetUtils.disjunction(expectedNodes, resultNodes);
            assertTrue(comparison.isEmpty(), "mismatch finding nodes by label for " + label);

            for (GraphNodeInMemory expectedNode : expectedNodes) {
                final GraphNodeInMemory resultNode = result.getNodeMutable(expectedNode.getId());
                assertEquals(expectedNode.getId(), resultNode.getId());
                assertEquals(expectedNode.getProperties(), resultNode.getProperties());

                checkRelationships(txnForExpected, Outgoing, expectedNode, result);
                checkRelationships(txnForExpected, Incoming, expectedNode, result);
            }
        }

        assertTrue(GraphCore.same(expected, result), "graph core same failed");
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
