package com.tramchester.integration.graph.inMemory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.inMemory.GraphNodeInMemory;
import com.tramchester.graph.core.inMemory.NodeIdInMemory;
import com.tramchester.graph.core.inMemory.SaveGraph;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
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
import java.util.EnumSet;

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

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldSerialiseToFileWithoutError() {
        SaveGraph saveGraph = componentContainer.get(SaveGraph.class);

        saveGraph.save(GRAPH_FILENAME);
        assertTrue(Files.exists(GRAPH_FILENAME));
    }

    @Test
    void shouldRoundTripGraphNode()  {
        GraphNodeId id = new NodeIdInMemory(678);
        EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.STATION, GraphLabel.INTERCHANGE);
        GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(id, labels);

        TramTime tramTime = TramTime.of(11, 42);
        graphNodeInMemory.setTime(tramTime);
        graphNodeInMemory.setLatLong(KnownLocations.nearBury.latLong());

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
        }
        catch(ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }

    }
}
