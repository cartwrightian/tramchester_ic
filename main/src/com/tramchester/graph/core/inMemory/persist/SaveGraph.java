package com.tramchester.graph.core.inMemory.persist;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.inMemory.GraphCore;
import com.tramchester.graph.core.inMemory.GraphIdFactory;
import com.tramchester.graph.core.inMemory.GraphInMemoryServiceManager;
import com.tramchester.graph.core.inMemory.NodesAndEdges;
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

    private final JsonMapper mapper;
    private final GraphInMemoryServiceManager serviceManager;

    @Inject
    public SaveGraph(GraphInMemoryServiceManager serviceManager) {
        this.serviceManager = serviceManager;
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

    /***
     * Creates own GraphIdFactory
     * @param graphFilename where to load from
     * @return a new instance of GraphCore
     */
    public static GraphCore loadDBFrom(final Path graphFilename) {
        logger.info("Load DB from " + graphFilename.toAbsolutePath() + " with file of size " + graphFilename.toFile().length());
        final NodesAndEdges nodesAndEdges = load(graphFilename);
        if (nodesAndEdges.isEmpty()) {
            throw new RuntimeException("Empty graph loaded from " + graphFilename.toAbsolutePath());
        }
        final GraphIdFactory graphIdFactory = new GraphIdFactory();
        return GraphCore.createFrom(nodesAndEdges, graphIdFactory);
    }

    public void save(final Path graphFilename) {
        GraphCore core = serviceManager.getGraphCore();
        final NodesAndEdges nodesAndEdges = core.getNodesAndEdges();

        if (nodesAndEdges.getNodes().isEmpty() || nodesAndEdges.getRelationships().isEmpty()) {
            String message = "Empty graph?? " + nodesAndEdges;
            logger.error(message);
            throw new RuntimeException(message);
        }

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
