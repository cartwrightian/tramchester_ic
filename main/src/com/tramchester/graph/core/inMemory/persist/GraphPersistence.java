package com.tramchester.graph.core.inMemory.persist;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.loader.files.TransportDataFromJSONFile;
import com.tramchester.graph.core.inMemory.*;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@LazySingleton
public class GraphPersistence {
    public static final Path RELATIONSHIPS_FILENAME = Path.of("graph_relationships.json");
    public static final Path NODES_FILENAME = Path.of("graph_nodes.json");
    private static final Logger logger = LoggerFactory.getLogger(GraphPersistence.class);

    private final JsonMapper mapper;

    @Inject
    public GraphPersistence() {
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
     * @param path folder to load from
     * @return a new instance of GraphCore
     */
    public GraphCore loadDBFrom(final Path path, final GraphIdFactory graphIdFactory) {
        logger.info("Load DB from folder " + path.toAbsolutePath());
        final NodesAndEdges nodesAndEdges = load(path);
        if (nodesAndEdges.isEmpty()) {
            throw new RuntimeException("Empty graph loaded from " + path.toAbsolutePath());
        }
        //final GraphIdFactory graphIdFactory = new GraphIdFactory();
        return GraphCore.createFrom(nodesAndEdges, graphIdFactory);
    }

    public boolean filesExistIn(final Path dbPath) {
        return Files.exists(dbPath.resolve(RELATIONSHIPS_FILENAME)) && Files.exists(dbPath.resolve(NODES_FILENAME));
    }

    // pass in GraphInMemoryServiceManager to avoid circular dependencies at create time
    public void save(final Path graphPath, GraphInMemoryServiceManager serviceManager) {
        if (!Files.exists(graphPath)) {
            try {
                Files.createDirectory(graphPath);
            } catch (IOException e) {
                String msg = "Could not create dir " + graphPath.toAbsolutePath();
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }
        } else {
            if (!Files.isDirectory(graphPath)) {
                String msg = "Is not a dir: " + graphPath.toAbsolutePath();
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        }

        final GraphCore core = serviceManager.getGraphCore();
        final NodesAndEdges nodesAndEdges = core.getNodesAndEdges();

        if (nodesAndEdges.getNodes().isEmpty() || nodesAndEdges.getRelationships().isEmpty()) {
            String message = "Empty graph?? " + nodesAndEdges;
            logger.error(message);
            throw new RuntimeException(message);
        }

        logger.info("Save graph to dir " + graphPath.toAbsolutePath());

        final Path relationshipsFile = graphPath.resolve(RELATIONSHIPS_FILENAME);
        try (final FileWriter output = new FileWriter(relationshipsFile.toFile())) {
            mapper.writeValue(output, nodesAndEdges.getRelationships());
            output.flush();
        } catch (IOException e) {
            String msg = "Unable to save relationships to " + relationshipsFile.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg,e);
        }
        logger.info("Saved relationships to " + relationshipsFile.toAbsolutePath());

        final Path nodesFile = graphPath.resolve(NODES_FILENAME);
        try (final FileWriter output = new FileWriter(nodesFile.toFile())) {
            mapper.writeValue(output, nodesAndEdges.getNodes());
            output.flush();
        } catch (IOException e) {
            String msg = "Unable to save nodes to " + nodesFile.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg,e);
        }
        logger.info("Saved nodes to " + relationshipsFile.toAbsolutePath());

        logger.info("Saved Graph");
    }

    public static NodesAndEdges load(final Path graphFilename) {
        logger.info("Load from " + graphFilename.toAbsolutePath());
        final JsonMapper jsonMapper = createMapper();

        final Path relationshipsFile = graphFilename.resolve(RELATIONSHIPS_FILENAME);
        final TransportDataFromJSONFile<GraphRelationshipInMemory> relationshipsLoader = new TransportDataFromJSONFile<>(relationshipsFile, GraphRelationshipInMemory.class, jsonMapper);

        final Path nodesFile = graphFilename.resolve(NODES_FILENAME);
        final TransportDataFromJSONFile<GraphNodeInMemory> nodesLoader = new TransportDataFromJSONFile<>(nodesFile, GraphNodeInMemory.class, jsonMapper);

        Stream<GraphRelationshipInMemory> relationships = relationshipsLoader.load();
        Stream<GraphNodeInMemory> nodes = nodesLoader.load();

        return new NodesAndEdges(relationships, nodes);

    }

}
