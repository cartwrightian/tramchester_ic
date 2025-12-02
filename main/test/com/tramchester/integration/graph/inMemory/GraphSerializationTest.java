package com.tramchester.integration.graph.inMemory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationshipId;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

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

        JsonFactory factory = JsonFactory.
                builder().
                configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, true).
                build();

        mapper = new ObjectMapper(factory);
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldSerialiseToFileWithoutError() {
        SaveGraph saveGraph = componentContainer.get(SaveGraph.class);

        saveGraph.save(GRAPH_FILENAME);
        assertTrue(Files.exists(GRAPH_FILENAME));

        Graph expected = componentContainer.get(Graph.class);

        NodesAndEdges result = saveGraph.load(GRAPH_FILENAME);
        assertEquals(expected.getCore(), result);
    }

    @Test
    void shouldRoundTripGraphNode()  {
        GraphNodeId id = new NodeIdInMemory(678);
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
        GraphNodeId idA = new NodeIdInMemory(678);
        GraphNodeId idB = new NodeIdInMemory(679);

        IdFor<Trip> tripA = Trip.createId("tripA");
        IdFor<Trip> tripB = Trip.createId("tripB");
        Duration cost = Duration.of(65, ChronoUnit.SECONDS);

        GraphRelationshipId id = new RelationshipIdInMemory(42);
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
}
