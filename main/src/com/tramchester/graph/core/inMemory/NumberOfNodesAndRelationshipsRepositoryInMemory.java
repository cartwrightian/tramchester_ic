package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.NumberOfNodesAndRelationshipsRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@LazySingleton
public class NumberOfNodesAndRelationshipsRepositoryInMemory implements NumberOfNodesAndRelationshipsRepository {
    private static final Logger logger = LoggerFactory.getLogger(NumberOfNodesAndRelationshipsRepositoryInMemory.class);

    private final GraphDatabase graphDatabase;
    private final Map<GraphLabel, Long> nodeCounts;
    private final Map<TransportRelationshipTypes, Long> relationshipCounts;

    @Inject
    public NumberOfNodesAndRelationshipsRepositoryInMemory(final GraphDatabase graphDatabase, StagedTransportGraphBuilder.Ready ready) {
        this.graphDatabase = graphDatabase;
        nodeCounts = new HashMap<>(GraphLabel.values().length);
        relationshipCounts = new HashMap<>(TransportRelationshipTypes.values().length);
    }

    @PostConstruct
    void start() {
        logger.info("start");

        if (graphDatabase instanceof GraphDatabaseNeo4J) {
            throw new RuntimeException("Wrong database type " + graphDatabase);
        }

        countNodeNumbers();
        countRelationships();

        logger.info("statrted");
    }

    private void countRelationships() {
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            for(TransportRelationshipTypes key : TransportRelationshipTypes.values()) {
                final long count = txn.numberOf(key);
                relationshipCounts.put(key, count);
                if (count>0) {
                    logger.info(count + " relationships of type " + key);
                }
            }
        }
    }

    private void countNodeNumbers() {
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            for(GraphLabel label : GraphLabel.values()) {
                final long count = txn.findNodes(label).count();
                nodeCounts.put(label, count);
                if (count>0) {
                    logger.info(count + " nodes of type " + label);
                }
            }
        }
    }

    @Override
    public Long numberOf(final GraphLabel label) {
        return nodeCounts.get(label);
    }

    @Override
    public long numberOf(final TransportRelationshipTypes relationshipType) {
        return relationshipCounts.get(relationshipType);
    }
}
