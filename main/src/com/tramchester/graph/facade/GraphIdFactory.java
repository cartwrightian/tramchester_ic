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

        final EnumSet<GraphLabel> labels;
        if (diagnostics) {
            // add labels to id to aid in diagnostics
            labels = GraphLabel.from(node.getLabels());
        } else {
            labels = EnumSet.noneOf(GraphLabel.class);
        }
        return nodeIds.computeIfAbsent(internalId, unused -> new GraphNodeId(internalId, labels));
    }

    GraphRelationshipId getIdFor(final Relationship relationship) {
        final String internalId = relationship.getElementId();
        return relationshipIds.computeIfAbsent(internalId, GraphRelationshipId::new);
    }

}
