package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
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
        TransportRelationshipTypes[] types = TransportRelationshipTypes.values();
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            for (TransportRelationshipTypes relationshipType : types) {
                long count = getCountFromQuery(txn,
                        "MATCH ()-[relationship:" + relationshipType.name() + "]->() " + "RETURN count(*) as count");
                relationshipCounts.put(relationshipType, count);
                if (count>0) {
                    logger.info(count + " relationships of type " + relationshipType.name());
                }
            }
        }
    }

    private void countNodeNumbers() {
        GraphLabel[] labels = GraphLabel.values();
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            for (GraphLabel label : labels) {
                long count = getCountFromQuery(txn,
                        "MATCH (node:" + label.name() + ") " + "RETURN count(*) as count");
                nodeCounts.put(label, count);
                if (count>0) {
                    logger.info(count + " nodes of type " + label.name());
                }
            }
        }
    }

    private long getCountFromQuery(GraphTransaction txn, String query) {
        Result result = txn.execute(query);
        ResourceIterator<Object> rows = result.columnAs("count");
        long count = (long) rows.next();
        result.close();
        return count;
    }

    @PreDestroy
    void stop() {
        nodeCounts.clear();
        relationshipCounts.clear();
    }

    public Long numberOf(GraphLabel label) {
        if (!nodeCounts.containsKey(label)) {
            return 0L;
        }
        return nodeCounts.get(label);
    }

    public long numberOf(TransportRelationshipTypes relationshipType) {
        return relationshipCounts.get(relationshipType);
    }
}
