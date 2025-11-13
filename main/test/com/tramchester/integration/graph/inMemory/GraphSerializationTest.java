package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.graph.core.inMemory.SaveGraph;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("WIP")
public class GraphSerializationTest {
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
    }
}
