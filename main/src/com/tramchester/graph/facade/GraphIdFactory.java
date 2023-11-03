package com.tramchester.graph.facade;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@LazySingleton
public class GraphIdFactory {
    private final ConcurrentMap<String, GraphNodeId> nodeIds;
    private final ConcurrentMap<String, GraphRelationshipId> relationshipIds;

    public GraphIdFactory() {
        nodeIds = new ConcurrentHashMap<>();
        relationshipIds = new ConcurrentHashMap<>();
    }

    GraphNodeId getIdFor(final Node node) {
        final String internalId = node.getElementId(); //node.getId();
        return nodeIds.computeIfAbsent(internalId, GraphNodeId::new);

    }

    GraphRelationshipId getIdFor(final Relationship relationship) {
        final String internalId = relationship.getElementId(); //relationship.getId();
        return relationshipIds.computeIfAbsent(internalId, GraphRelationshipId::new);
    }

    GraphNodeId getNodeIdFor(final String legacyId) {
        return nodeIds.computeIfAbsent(legacyId, GraphNodeId::new);
    }
}
