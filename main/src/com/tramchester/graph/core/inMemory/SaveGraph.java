package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

@LazySingleton
public class SaveGraph {
    private static final Logger logger = LoggerFactory.getLogger(SaveGraph.class);

    private final Graph graph;

    @Inject
    public SaveGraph(Graph graph) {
        this.graph = graph;
    }

    public void save(final Path graphFilename) {
        final JsonMapper mapper = JsonMapper.builder().
                addModule(new JavaTimeModule()).
                build();

        logger.info("Save graph to " + graphFilename.toAbsolutePath());
        try (FileWriter output = new FileWriter(graphFilename.toFile())) {
            mapper.writeValue(output, graph);
            logger.info("Saved");
        } catch (IOException e) {
            logger.error("Unable to save graph to " + graphFilename, e);
        }
    }


}
