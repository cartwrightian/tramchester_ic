package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@JsonPropertyOrder({"nodes", "relationships"})
@LazySingleton
public class Graph {
    private static final Logger logger = LoggerFactory.getLogger(Graph.class);

    private final AtomicInteger nextGraphNodeId;
    private final AtomicInteger nextRelationshipId;

    private final NodesAndEdges nodesAndEdges;

    private final ConcurrentMap<GraphNodeId, RelationshipsForNode> relationshipsForNodes;
    private final ConcurrentMap<NodeIdPair, EnumSet<TransportRelationshipTypes>> existingRelationships;
    private final ConcurrentMap<GraphLabel, Set<NodeIdInMemory>> labelsToNodes;
    private final ConcurrentMap<TransportRelationshipTypes, AtomicInteger> relationshipTypeCounts;

    // todo proper transaction handling, rollbacks etc

    public Graph() {
        nextGraphNodeId = new AtomicInteger(0);
        nextRelationshipId = new AtomicInteger(0);
        nodesAndEdges = new NodesAndEdges();

        relationshipsForNodes = new ConcurrentHashMap<>();
        labelsToNodes = new ConcurrentHashMap<>();
        relationshipTypeCounts = new ConcurrentHashMap<>();
        existingRelationships = new ConcurrentHashMap<>();
    }

    public static Graph createFrom(final NodesAndEdges incoming) {
        Graph result = new Graph();
        result.start();

        incoming.getNodes().forEach(node -> {
            // update highest node id

            final NodeIdInMemory id = node.getId();
            result.nodesAndEdges.addNode(id, node);

            EnumSet<GraphLabel> labels = node.getLabels();
            labels.forEach(label -> result.labelsToNodes.get(label).add(id));
        });
        // todo push in
        result.nodesAndEdges.updateHighestId(result.nextGraphNodeId);

        return result;
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        for(GraphLabel label : GraphLabel.values()) {
            labelsToNodes.put(label, new HashSet<>());
        }
        for(TransportRelationshipTypes type : TransportRelationshipTypes.values()) {
            relationshipTypeCounts.put(type, new AtomicInteger(0));
        }
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stop");
        logger.error("reinstate");
        nextGraphNodeId.set(0);
        nextRelationshipId.set(0);

        nodesAndEdges.clear();

        relationshipsForNodes.clear();
        labelsToNodes.clear();
        logger.info("stopped");
    }

    public synchronized GraphNodeInMemory createNode(final EnumSet<GraphLabel> labels) {
        final int id = nextGraphNodeId.getAndIncrement();
        final GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(new NodeIdInMemory(id), labels);
        final NodeIdInMemory graphNodeId = graphNodeInMemory.getId();
        nodesAndEdges.addNode(graphNodeId, graphNodeInMemory);
        labels.forEach(label -> labelsToNodes.get(label).add(graphNodeId));
        return graphNodeInMemory;
    }

    public synchronized GraphRelationshipInMemory createRelationship(final TransportRelationshipTypes relationshipType,
                                                                     final GraphNodeInMemory begin,
                                                                     final GraphNodeInMemory end) {
        checkAndUpdateExistingRelationships(relationshipType, begin, end);

        final int id = nextRelationshipId.getAndIncrement();
        final GraphRelationshipInMemory relationship = new GraphRelationshipInMemory(relationshipType,
                new RelationshipIdInMemory(id), begin.getId(), end.getId());
        final RelationshipIdInMemory relationshipId = relationship.getId();

        nodesAndEdges.addRelationship(relationshipId, relationship);
        addOutboundTo(begin.getId(), relationshipId);
        addInboundTo(end.getId(), relationshipId);
        relationshipTypeCounts.get(relationshipType).getAndIncrement();
        return relationship;
    }

    private void checkAndUpdateExistingRelationships(final TransportRelationshipTypes relationshipType, final GraphNodeInMemory begin,
                                                     final GraphNodeInMemory end) {
        final NodeIdPair idPair = NodeIdPair.of(begin, end);
        if (existingRelationships.containsKey(idPair)) {
            if (existingRelationships.get(idPair).contains(relationshipType)) {
                String message = "Already have relationship of type " + relationshipType + " between " + begin + " and " + end;
                logger.error(message);
                throw new RuntimeException(message);
            } else {
                existingRelationships.get(idPair).add(relationshipType);
            }
        } else {
            existingRelationships.put(idPair, EnumSet.of(relationshipType));
        }
    }

    private void addInboundTo(final GraphNodeId id, final RelationshipIdInMemory relationshipId) {
        if (!relationshipsForNodes.containsKey(id)) {
            relationshipsForNodes.put(id, new RelationshipsForNode());
        }
        relationshipsForNodes.get(id).addInbound(relationshipId);
    }

    private void addOutboundTo(final GraphNodeId id, final RelationshipIdInMemory relationshipId) {
        if (!relationshipsForNodes.containsKey(id)) {
            relationshipsForNodes.put(id, new RelationshipsForNode());
        }
        relationshipsForNodes.get(id).addOutbound(relationshipId);
    }

    public Stream<GraphRelationshipInMemory> getRelationshipsFor(final NodeIdInMemory id, final GraphDirection direction) {
        if (!nodesAndEdges.hasNode(id)) {
            String msg = "No such node " + id;
            logger.error(msg);
            throw new GraphException(msg);
        }
        if (relationshipsForNodes.containsKey(id)) {
            final RelationshipsForNode relationshipsForNode = relationshipsForNodes.get(id);
            return switch (direction) {
                case Outgoing -> nodesAndEdges.getOutbounds(relationshipsForNode);
                case Incoming -> nodesAndEdges.getInbounds(relationshipsForNode);
                case Both -> Stream.concat(nodesAndEdges.getOutbounds(relationshipsForNode),
                        nodesAndEdges.getInbounds(relationshipsForNode));
            };
        } else {
            logger.info("node " + id + " has not relationships");
            return Stream.empty();
        }
    }

    synchronized void delete(final RelationshipIdInMemory id) {
        if (!nodesAndEdges.hasRelationship(id)) {
            String msg = "Cannot delete relationship, missing id " + id;
            logger.error(msg);
            throw new GraphException(msg);
        }
        final GraphRelationshipInMemory relationship = nodesAndEdges.getRelationship(id);
        final GraphNodeId begin = relationship.getStartId();
        final GraphNodeId end = relationship.getEndId();

        deleteRelationshipFrom(begin, id);
        deleteRelationshipFrom(end, id);

        relationshipTypeCounts.get(relationship.getType()).getAndDecrement();
    }

    synchronized void delete(final NodeIdInMemory id) {
        if (!nodesAndEdges.hasNode(id)) {
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
        final EnumSet<GraphLabel> labels = nodesAndEdges.getNode(id).getLabels();
        labels.forEach(label -> labelsToNodes.get(label).remove(id));
        // the node
        nodesAndEdges.removeNode(id);
    }

    private void deleteRelationshipFrom(final GraphNodeId graphNodeId, final RelationshipIdInMemory relationshipId) {
        if (relationshipsForNodes.containsKey(graphNodeId)) {
            RelationshipsForNode relationshipsForNode = relationshipsForNodes.get(graphNodeId);
            relationshipsForNode.remove(relationshipId);
        }
    }

    public GraphNodeInMemory getNode(final NodeIdInMemory nodeId) {
        if (!nodesAndEdges.hasNode(nodeId)) {
            String msg = "No such node " + nodeId;
            logger.error(msg);
            throw new GraphException(msg);
        }
        return nodesAndEdges.getNode(nodeId);
    }

    public Stream<GraphNodeInMemory> findNodes(final GraphLabel graphLabel) {
        final Set<NodeIdInMemory> matchingIds = labelsToNodes.get(graphLabel);
        return matchingIds.stream().map(nodesAndEdges::getNode);
    }

    GraphRelationship getRelationship(final RelationshipIdInMemory graphRelationshipId) {
        if (nodesAndEdges.hasRelationship(graphRelationshipId)) {
            return nodesAndEdges.getRelationship(graphRelationshipId);
        } else {
            String msg = "No such relationship " + graphRelationshipId;
            logger.error(msg);
            throw new GraphException(msg);
        }
    }

    synchronized void addLabel(final NodeIdInMemory id, final GraphLabel label) {
        labelsToNodes.get(label).add(id);
    }

    @Override
    public String toString() {
        return "Graph{" +
                "nextGraphNodeId=" + nextGraphNodeId +
                ", nextRelationshipId=" + nextRelationshipId +
                ", nodesAndEdges=" + nodesAndEdges +
                ", relationshipsForNodes=" + relationshipsForNodes +
                ", labelsToNodes=" + labelsToNodes +
                '}';
    }

    @JsonIgnore
    public long getNumberOf(TransportRelationshipTypes relationshipType) {
        return relationshipTypeCounts.get(relationshipType).get();
    }

    public NodesAndEdges getCore() {
        return nodesAndEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Graph graph = (Graph) o;
        return Objects.equals(nodesAndEdges, graph.nodesAndEdges) &&
                Objects.equals(relationshipsForNodes, graph.relationshipsForNodes) &&
                Objects.equals(existingRelationships, graph.existingRelationships) &&
                Objects.equals(labelsToNodes, graph.labelsToNodes) &&
                Objects.equals(relationshipTypeCounts, graph.relationshipTypeCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodesAndEdges, relationshipsForNodes, existingRelationships, labelsToNodes, relationshipTypeCounts);
    }

    private static class NodeIdPair {
        private final GraphNodeId first;
        private final GraphNodeId second;

        public static NodeIdPair of(GraphNodeInMemory begin, GraphNodeInMemory end) {
            return new NodeIdPair(begin.getId(), end.getId());
        }

        private NodeIdPair(GraphNodeId first, GraphNodeId second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            NodeIdPair that = (NodeIdPair) o;
            return Objects.equals(first, that.first) && Objects.equals(second, that.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
}
