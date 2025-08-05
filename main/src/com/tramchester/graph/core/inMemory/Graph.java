package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

@LazySingleton
public class Graph {
    private final AtomicInteger graphNodeId;
    private final AtomicInteger relationshipId;

    // todo proper transaction handling, rollbacks etc

    public Graph() {
        graphNodeId = new AtomicInteger(0);
        relationshipId = new AtomicInteger(0);
    }

    public synchronized GraphNodeInMemory createNode(final EnumSet<GraphLabel> labels) {
        int id = graphNodeId.getAndIncrement();
        return new GraphNodeInMemory(new NodeIdInMemory(id), labels);
    }

    public synchronized GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType,
                                                                     MutableGraphNode begin, MutableGraphNode end) {
        int id = relationshipId.getAndIncrement();
        return new GraphRelationshipInMemory(relationshipType, new RelationshipIdInMemory(id), begin, end);
    }
}
