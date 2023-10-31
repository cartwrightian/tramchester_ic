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

    public GraphNodeId getIdFor(Node node) {
        long internalId = node.getId();
        return nodeIds.computeIfAbsent(internalId, GraphNodeId::new);

    }

    public GraphRelationshipId getIdFor(Relationship relationship) {
        long internalId = relationship.getId();
        return relationshipIds.computeIfAbsent(internalId, GraphRelationshipId::new);
    }
}
