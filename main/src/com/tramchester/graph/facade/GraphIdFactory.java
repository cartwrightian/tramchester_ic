package com.tramchester.graph.facade;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/// TODO use a caffeine based cache?
public class GraphIdFactory {
    private static final EnumSet<GraphLabel> NO_LABELS = EnumSet.noneOf(GraphLabel.class);

    private final ConcurrentMap<String, GraphNodeId> nodeIds;
    private final ConcurrentMap<String, GraphRelationshipId> relationshipIds;
    private final boolean diagnostics;

    public GraphIdFactory(final GraphDBConfig graphDBConfig) {
        nodeIds = new ConcurrentHashMap<>();
        relationshipIds = new ConcurrentHashMap<>();
        diagnostics = graphDBConfig.enableDiagnostics();
    }

    GraphNodeId getIdFor(final Node node) {
        final String internalId = node.getElementId();

        if (diagnostics) {
            // add labels to id to aid in diagnostics
            final EnumSet<GraphLabel> labels = GraphLabel.from(node.getLabels());
            return nodeIds.computeIfAbsent(internalId, unused -> new GraphNodeId(internalId, labels));
        } else {
            return nodeIds.computeIfAbsent(internalId, unused -> new GraphNodeId(internalId, NO_LABELS));
        }
    }

    GraphRelationshipId getIdFor(final Relationship relationship) {
        final String internalId = relationship.getElementId();
        return relationshipIds.computeIfAbsent(internalId, GraphRelationshipId::new);
    }

}
