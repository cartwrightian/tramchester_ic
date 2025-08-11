package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphRelationshipId;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@LazySingleton
public class Graph {
    private static final Logger logger = LoggerFactory.getLogger(Graph.class);

    private final AtomicInteger nextGraphNodeId;
    private final AtomicInteger nextRelationshipId;
    private final ConcurrentMap<GraphRelationshipId, GraphRelationshipInMemory> relationships;
    private final ConcurrentMap<GraphNodeId, GraphNodeInMemory> nodes;
    private final ConcurrentMap<GraphNodeId, RelationshipsForNode> relationshipsForNodes;
    private final ConcurrentMap<GraphLabel, Set<GraphNodeId>> labelsToNodes;

    // todo proper transaction handling, rollbacks etc

    public Graph() {
        nextGraphNodeId = new AtomicInteger(0);
        nextRelationshipId = new AtomicInteger(0);
        relationships = new ConcurrentHashMap<>();
        nodes = new ConcurrentHashMap<>();
        relationshipsForNodes = new ConcurrentHashMap<>();
        labelsToNodes = new ConcurrentHashMap<>();
        for(GraphLabel label : GraphLabel.values()) {
            labelsToNodes.put(label, new HashSet<>());
        }
    }

    public synchronized GraphNodeInMemory createNode(final EnumSet<GraphLabel> labels) {
        int id = nextGraphNodeId.getAndIncrement();
        final GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(new NodeIdInMemory(id), labels);
        final GraphNodeId graphNodeId = graphNodeInMemory.getId();
        nodes.put(graphNodeId, graphNodeInMemory);
        labels.forEach(label -> labelsToNodes.get(label).add(graphNodeId));
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
        if (!nodes.containsKey(id)) {
            String msg = "No such node " + id;
            logger.error(msg);
            throw new GraphException(msg);
        }
        if (relationshipsForNodes.containsKey(id)) {
            RelationshipsForNode relationshipsForNode = relationshipsForNodes.get(id);
            return switch (direction) {
                case Outgoing -> relationshipsForNode.getOutbound(relationships);
                case Incoming -> relationshipsForNode.getInbound(relationships);
                case Both -> Stream.concat(relationshipsForNode.getOutbound(relationships), relationshipsForNode.getInbound(relationships));
            };
        } else {
            logger.info("node " + id + " has not relationships");
            return Stream.empty();
        }
    }

    public synchronized void delete(final GraphRelationshipId id) {
        if (!relationships.containsKey(id)) {
            String msg = "Cannot delete relationship, missing id " + id;
            logger.error(msg);
            throw new GraphException(msg);
        }
        final GraphRelationshipInMemory relationship = relationships.get(id);
        GraphNodeId begin = relationship.getStartId();
        GraphNodeId end = relationship.getEndId();

        deleteRelationshipFrom(begin, id);
        deleteRelationshipFrom(end, id);
    }

    public synchronized void delete(final GraphNodeId id) {
        if (!nodes.containsKey(id)) {
            String msg = "Missing id " + id;
            logger.error(msg);
            throw new GraphException(msg);
        }
        // relationships
        if (relationshipsForNodes.containsKey(id)) {
            final RelationshipsForNode forNode = relationshipsForNodes.get(id);
            if (!forNode.isEmpty()) {
                String msg = "Node " + id + " still has relationships " + relationshipsForNodes;
                logger.error(msg);
                throw new GraphException(msg);
            }
        }
        // label map
        final EnumSet<GraphLabel> labels = nodes.get(id).getLabels();
        labels.forEach(label -> labelsToNodes.get(label).remove(id));
        // the node
        nodes.remove(id);
    }

    private void deleteRelationshipFrom(final GraphNodeId graphNodeId, final GraphRelationshipId relationshipId) {
        if (relationshipsForNodes.containsKey(graphNodeId)) {
            relationshipsForNodes.get(graphNodeId).remove(relationshipId);
        }
    }

    public GraphNodeInMemory getNode(final GraphNodeId nodeId) {
        if (!nodes.containsKey(nodeId)) {
            String msg = "No such node " + nodeId;
            logger.error(msg);
            throw new GraphException(msg);
        }
        return nodes.get(nodeId);
    }

    public Stream<GraphNodeInMemory> findNodes(final GraphLabel graphLabel) {
        final Set<GraphNodeId> matchingIds = labelsToNodes.get(graphLabel);
        return matchingIds.stream().map(nodes::get);
        //return nodes.values().stream().filter(node -> node.hasLabel(graphLabel));
    }

    public GraphRelationship getRelationship(final GraphRelationshipId graphRelationshipId) {
        if (relationships.containsKey(graphRelationshipId)) {
            return relationships.get(graphRelationshipId);
        } else {
            String msg = "No such relationship " + graphRelationshipId;
            logger.error(msg);
            throw new GraphException(msg);
        }
    }

    public synchronized void addLabel(final GraphNodeId id, final GraphLabel label) {
        labelsToNodes.get(label).add(id);
    }

    public Stream<GraphRelationship> findRelationships(final TransportRelationshipTypes relationshipType) {
        // TODO inefficient
        return relationships.values().stream().
                filter(graphRelationshipInMemory -> graphRelationshipInMemory.isType(relationshipType)).
                map(item -> item);
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

        public void addOutbound(final GraphRelationshipId relationshipId) {
            synchronized (outbound) {
                outbound.add(relationshipId);
            }
        }

        public void addInbound(final GraphRelationshipId relationshipId) {
            synchronized (inbound) {
                inbound.add(relationshipId);
            }
        }

        public void remove(final GraphRelationshipId relationshipId) {
            synchronized (outbound) {
                outbound.remove(relationshipId);
            }
            synchronized (inbound) {
                inbound.remove(relationshipId);
            }
        }

        public boolean isEmpty() {
            return outbound.isEmpty() && inbound.isEmpty();
        }

        @Override
        public String toString() {
            return "RelationshipsForNode{" +
                    "outbound=" + outbound +
                    ", inbound=" + inbound +
                    '}';
        }
    }
}
