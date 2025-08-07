package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@LazySingleton
public class Graph {
    private final AtomicInteger nextGraphNodeId;
    private final AtomicInteger nextRelationshipId;
    private final ConcurrentMap<GraphRelationshipId, GraphRelationshipInMemory> relationships;
    private final ConcurrentMap<GraphNodeId, GraphNodeInMemory> nodes;
    private final ConcurrentMap<GraphNodeId, RelationshipsForNode> relationshipsForNodes;

    // todo proper transaction handling, rollbacks etc

    public Graph() {
        nextGraphNodeId = new AtomicInteger(0);
        nextRelationshipId = new AtomicInteger(0);
        relationships = new ConcurrentHashMap<>();
        nodes = new ConcurrentHashMap<>();
        relationshipsForNodes = new ConcurrentHashMap<>();
    }

    public synchronized GraphNodeInMemory createNode(final EnumSet<GraphLabel> labels) {
        int id = nextGraphNodeId.getAndIncrement();
        final GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(new NodeIdInMemory(id), labels);
        final GraphNodeId graphNodeId = graphNodeInMemory.getId();
        nodes.put(graphNodeId, graphNodeInMemory);
        return graphNodeInMemory;
    }

    public synchronized GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType,
                                                                     GraphNodeInMemory begin, GraphNodeInMemory end) {
        final int id = nextRelationshipId.getAndIncrement();
        final GraphRelationshipInMemory relationship = new GraphRelationshipInMemory(relationshipType, new RelationshipIdInMemory(id), begin, end);
        final GraphRelationshipId relationshipId = relationship.getId();
        relationships.put(relationshipId, relationship);
        addOutboundTo(begin.getId(), relationshipId);
        addInboundTo(end.getId(), relationshipId);
        return relationship;
    }

    private void addInboundTo(GraphNodeId id, GraphRelationshipId relationshipId) {
        if (!relationshipsForNodes.containsKey(id)) {
            relationshipsForNodes.put(id, new RelationshipsForNode());
        }
        relationshipsForNodes.get(id).addInbound(relationshipId);
    }

    private void addOutboundTo(GraphNodeId id, GraphRelationshipId relationshipId) {
        if (!relationshipsForNodes.containsKey(id)) {
            relationshipsForNodes.put(id, new RelationshipsForNode());
        }
        relationshipsForNodes.get(id).addOutbound(relationshipId);
    }

    public Stream<GraphRelationshipInMemory> getRelationshipsFor(final GraphNodeId id, final GraphDirection direction) {
        if (relationshipsForNodes.containsKey(id)) {
            RelationshipsForNode relationshipsForNode = relationshipsForNodes.get(id);
            return switch (direction) {
                case Outgoing -> relationshipsForNode.getOutbound(relationships);
                case Incoming -> relationshipsForNode.getInbound(relationships);
                case Both -> Stream.concat(relationshipsForNode.getOutbound(relationships), relationshipsForNode.getInbound(relationships));
            };
        } else {
            return Stream.empty();
        }
    }

    private static class RelationshipsForNode {
        private final Set<GraphRelationshipId> outbound;
        private final Set<GraphRelationshipId> inbound;

        private RelationshipsForNode() {
            outbound = new HashSet<>();
            inbound = new HashSet<>();
        }

        public Stream<GraphRelationshipInMemory> getOutbound(ConcurrentMap<GraphRelationshipId, GraphRelationshipInMemory> relationships) {
            return outbound.stream().map(relationships::get);
        }

        public Stream<GraphRelationshipInMemory> getInbound(ConcurrentMap<GraphRelationshipId, GraphRelationshipInMemory> relationships) {
            return inbound.stream().map(relationships::get);
        }

        public void addOutbound(GraphRelationshipId relationshipId) {
            outbound.add(relationshipId);
        }

        public void addInbound(GraphRelationshipId relationshipId) {
            inbound.add(relationshipId);
        }
    }
}
