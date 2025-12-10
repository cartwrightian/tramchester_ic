package com.tramchester.integration.graph.inMemory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("WIP - memory issues via gradle")
public class GraphSerializationTest {
    private static final Path GRAPH_FILENAME = Path.of("graph_test.json");
    private static GuiceContainerDependencies componentContainer;
    private ObjectMapper mapper;

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

        mapper = SaveGraph.createMapper();
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

        Graph graphA = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);
        GraphDatabaseInMemory graphDatabase = createGraphDatabaseInMemory(graphA);

        graphDatabase.start();

        // A-> A
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(graphA, graphA, txn);
        }
    }

    @Test
    void shouldLoadConsistently() {

        Graph graphA = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);
        Graph graphB = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_OK);

        GraphDatabaseInMemory graphDatabase = createGraphDatabaseInMemory(graphA);
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

        GraphDatabaseInMemory graphDatabase = createGraphDatabaseInMemory(graphA);
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
                checkRelationships(txnForExpected, GraphDirection.Incoming, expectedNode, result);
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

    @Test
    void shouldRoundTripGraphNode()  {
        NodeIdInMemory id = new NodeIdInMemory(678);
        EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.STATION, GraphLabel.INTERCHANGE);
        GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(id, labels);

        TramTime tramTime = TramTime.of(11, 42);
        graphNodeInMemory.setTime(tramTime);
        graphNodeInMemory.setLatLong(KnownLocations.nearBury.latLong());
        graphNodeInMemory.set(TestEnv.getTramTestRoute());
        graphNodeInMemory.setTransportMode(Tram);

        String text = null;
        try {
            text = mapper.writeValueAsString(graphNodeInMemory);
        } catch (JsonProcessingException e) {
            fail("failed to serialise", e);
        }

        GraphNodeInMemory result = null;
        try {
            result = mapper.readValue(text, GraphNodeInMemory.class);
        } catch (JsonProcessingException e) {
            fail("Unable to deserialize " + text,e);
        }

        assertEquals(graphNodeInMemory, result);

        try {
            assertEquals(tramTime, result.getTime());
            assertEquals(KnownLocations.nearBury.latLong(), result.getLatLong());
            assertEquals(TestEnv.getTramTestRoute().getId(), result.getRouteId());
            assertEquals(Tram, result.getTransportMode());
        }
        catch(ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }

    }

    @Test
    void shouldRoundTripGraphRelationship()  {
        NodeIdInMemory idA = new NodeIdInMemory(678);
        NodeIdInMemory idB = new NodeIdInMemory(679);

        IdFor<Trip> tripA = Trip.createId("tripA");
        IdFor<Trip> tripB = Trip.createId("tripB");
        Duration cost = Duration.of(65, ChronoUnit.SECONDS);

        RelationshipIdInMemory id = new RelationshipIdInMemory(42);
        GraphRelationshipInMemory relationship = new GraphRelationshipInMemory(TransportRelationshipTypes.BOARD, id,
                idA, idB);

        TramTime tramTime = TramTime.of(11, 42);
        relationship.setTime(tramTime);
        relationship.set(TestEnv.getTramTestRoute());
        relationship.setHour(17);
        relationship.setStopSeqNum(42);
        relationship.setCost(cost);
        relationship.addTripId(tripA);
        relationship.addTripId(tripB);
        relationship.addTransportMode(Bus);
        relationship.addTransportMode(Tram);

        String text = null;
        try {
            text = mapper.writeValueAsString(relationship);
        } catch (JsonProcessingException e) {
            fail("failed to serialise", e);
        }

        GraphRelationshipInMemory result = null;
        try {
            result = mapper.readValue(text, GraphRelationshipInMemory.class);
        } catch (JsonProcessingException e) {
            fail("Unable to deserialize " + text,e);
        }

        assertEquals(relationship, result);

        try {
            assertEquals(tramTime, result.getTime());
            assertEquals(TestEnv.getTramTestRoute().getId(), result.getRouteId());
            assertEquals(17, relationship.getHour());
            assertEquals(42, relationship.getStopSeqNumber());
            assertEquals(cost, relationship.getCost());
            IdSet<Trip> trips = relationship.getTripIds();
            assertEquals(2, trips.size());
            assertTrue(trips.contains(tripA));
            assertTrue(trips.contains(tripB));
            assertEquals(EnumSet.of(Bus,Tram),relationship.getTransportModes());

        }
        catch(ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }

    }

    private static @NotNull GraphDatabaseInMemory createGraphDatabaseInMemory(Graph graphA) {
        ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);
        TransactionManager transactionManager = new TransactionManager(providesNow, graphA);
        return new GraphDatabaseInMemory(transactionManager);
    }
}
