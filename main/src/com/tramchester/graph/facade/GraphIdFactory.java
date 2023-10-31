package com.tramchester.graph.facade;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@LazySingleton
public class GraphIdFactory {
    private final ConcurrentMap<Long, GraphNodeId> nodeIds;
    private final ConcurrentMap<Long, GraphRelationshipId> relationshipIds;

    public GraphIdFactory() {
        nodeIds = new ConcurrentHashMap<>();
        relationshipIds = new ConcurrentHashMap<>();
    }

    GraphNodeId getIdFor(final Node node) {
        final long internalId = node.getId();
        return nodeIds.computeIfAbsent(internalId, GraphNodeId::new);

    }

    GraphRelationshipId getIdFor(final Relationship relationship) {
        final long internalId = relationship.getId();
        return relationshipIds.computeIfAbsent(internalId, GraphRelationshipId::new);
    }

    GraphNodeId getNodeIdFor(final long legacyId) {
        return nodeIds.computeIfAbsent(legacyId, GraphNodeId::new);
    }
}
