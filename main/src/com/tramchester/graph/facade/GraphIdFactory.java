package com.tramchester.graph.facade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class GraphIdFactory implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(GraphIdFactory.class);

    private static final EnumSet<GraphLabel> NO_LABELS = EnumSet.noneOf(GraphLabel.class);

    private final Cache<String, GraphNodeId> nodeIds;
    private final Cache<String, GraphRelationshipId> relationshipIds;
    private final boolean diagnostics;

    public GraphIdFactory(final boolean diagnostics) {
        this.diagnostics = diagnostics;

        nodeIds = createCache();
        relationshipIds = createCache();
        logger.info("created");
    }

    private <K, V> Cache<K, V>  createCache() {
        return Caffeine.newBuilder().
                //expireAfterAccess(DEFAULT_EXPIRY).
                initialCapacity(20000).
                recordStats().
                build();
    }

    GraphNodeId getIdFor(final Node node) {
        final String internalId = node.getElementId();

        if (diagnostics) {
            // add labels to id to aid in diagnostics
            final EnumSet<GraphLabel> labels = GraphLabel.from(node.getLabels());
            return nodeIds.get(internalId, unused -> new GraphNodeId(internalId, labels));
        } else {
            return nodeIds.get(internalId, unused -> new GraphNodeId(internalId, NO_LABELS));
        }
    }

    GraphRelationshipId getIdFor(final Relationship relationship) {
        final String internalId = relationship.getElementId();
        return relationshipIds.get(internalId, GraphRelationshipId::new);
    }

    public void close() {
        stats().forEach(pair -> logger.info("Cache stats for " + pair.getLeft() + " " + pair.getRight().toString()));
        relationshipIds.invalidateAll();
        nodeIds.invalidateAll();
        logger.info("closed");
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        final List<Pair<String, CacheStats>> results = new ArrayList<>();
        results.add(Pair.of("nodeIds", nodeIds.stats()));
        results.add(Pair.of("relationshipIds", relationshipIds.stats()));

        return results;
    }

}
