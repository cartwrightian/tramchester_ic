package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

@LazySingleton
public class SaveGraph {
    private static final Logger logger = LoggerFactory.getLogger(SaveGraph.class);

    private final Graph graph;
    private final JsonMapper mapper;

    @Inject
    public SaveGraph(Graph graph) {
        this.graph = graph;

        mapper = JsonMapper.builder().
                addModule(new JavaTimeModule()).
                build();
    }

    public void save(final Path graphFilename) {

        final NodesAndEdges nodesAndEdges = graph.getCore();

        logger.info("Save graph to " + graphFilename.toAbsolutePath());

        try (final FileWriter output = new FileWriter(graphFilename.toFile())) {
            mapper.writeValue(output, nodesAndEdges);
            logger.info("Saved");

        } catch (IOException e) {
            String msg = "Unable to save graph to " + graphFilename.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg,e);
        }
    }


    public NodesAndEdges load(final Path graphFilename) {
        logger.info("Load from " + graphFilename.toAbsolutePath());

        try(final FileReader reader = new FileReader(graphFilename.toFile())) {
             return mapper.readValue(reader, NodesAndEdges.class);
        } catch (IOException e) {
            String msg = "Unable to load graph from " + graphFilename.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
