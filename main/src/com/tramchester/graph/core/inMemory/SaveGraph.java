package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
        this.mapper = createMapper();
    }

    public static JsonMapper createMapper() {
        return JsonMapper.builder().
                enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).
                // we need to make sure flush called before close
                disable(SerializationFeature.CLOSE_CLOSEABLE).
                disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET).
                addModule(new JavaTimeModule()).
                build();
    }

    public static Graph loadDBFrom(final Path graphFilename) {
        logger.info("Load DB from " + graphFilename.toAbsolutePath() + " with file of size " + graphFilename.toFile().length());
        final NodesAndEdges nodesAndEdges = load(graphFilename);
        if (nodesAndEdges.isEmpty()) {
            throw new RuntimeException("Empty graph loaded from " + graphFilename.toAbsolutePath());
        }
        return Graph.createFrom(nodesAndEdges);
    }

    public void save(final Path graphFilename) {

        final NodesAndEdges nodesAndEdges = graph.getCore();

        logger.info("Save graph to " + graphFilename.toAbsolutePath());

        final File file = graphFilename.toFile();
        try (final FileWriter output = new FileWriter(file)) {
            mapper.writeValue(output, nodesAndEdges);
            output.flush();
        } catch (IOException e) {
            String msg = "Unable to save graph to " + graphFilename.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg,e);
        }
        logger.info("Saved");
    }


    // TODO ideally want a stream version of this to avoid large memory consumption
    public static NodesAndEdges load(final Path graphFilename) {
        logger.info("Load from " + graphFilename.toAbsolutePath());
        final JsonMapper jsonMapper = createMapper();

        try(final FileReader reader = new FileReader(graphFilename.toFile())) {
             return jsonMapper.readValue(reader, NodesAndEdges.class);
        } catch (IOException e) {
            String msg = "Unable to load graph from " + graphFilename.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
