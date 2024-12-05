package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import jakarta.inject.Inject;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;


@LazySingleton
public
class NumberOfNodesAndRelationshipsRepository {
    private static final Logger logger = LoggerFactory.getLogger(NumberOfNodesAndRelationshipsRepository.class);

    private final Map<GraphLabel, Long> nodeCounts;
    private final Map<TransportRelationshipTypes, Long> relationshipCounts;
    private final GraphDatabase graphDatabase;

    @Inject
    public NumberOfNodesAndRelationshipsRepository(GraphDatabase graphDatabase, StagedTransportGraphBuilder.Ready ready) {
        this.graphDatabase = graphDatabase;
        nodeCounts = new HashMap<>(GraphLabel.values().length);
        relationshipCounts = new HashMap<>(TransportRelationshipTypes.values().length);
    }

    @PostConstruct
    void start() {
        logger.info("start");

        countNodeNumbers();
        countRelationships();

        logger.info("statrted");
    }

    private void countRelationships() {
        // note cypher does not allow parameters for labels
        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            for (final TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
                final String query = "MATCH ()-[relationship:" + relationshipType.name() + "]->() " + "RETURN count(relationship) as count";
                final long count = getCountFromQuery(txn, query);
                relationshipCounts.put(relationshipType, count);
                if (count>0) {
                    logger.info(count + " relationships of type " + relationshipType.name());
                }
            }
        }
    }

    private void countNodeNumbers() {
        // note cypher does not allow parameters for labels
        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            for (final GraphLabel label : GraphLabel.values()) {
                final String query = "MATCH (node:" + label.name() + ") " + "RETURN count(node) as count";
                final long count = getCountFromQuery(txn, query);
                nodeCounts.put(label, count);
                if (count>0) {
                    logger.info(count + " nodes of type " + label.name());
                }
            }
        }
    }


    public long numberOfNodes() {
        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            return getCountFromQuery(txn, "MATCH (n)\n" +
                    "RETURN count(n) as count");
        }
    }

    private long getCountFromQuery(final MutableGraphTransaction txn, final String query) {
        final Result result = txn.execute(query);
        final ResourceIterator<Object> rows = result.columnAs("count");
        final long count = (long) rows.next();
        result.close();
        return count;
    }

    @PreDestroy
    void stop() {
        nodeCounts.clear();
        relationshipCounts.clear();
    }

    public Long numberOf(final GraphLabel label) {
        if (!nodeCounts.containsKey(label)) {
            return 0L;
        }
        return nodeCounts.get(label);
    }

    public long numberOf(final TransportRelationshipTypes relationshipType) {
        return relationshipCounts.get(relationshipType);
    }

}
