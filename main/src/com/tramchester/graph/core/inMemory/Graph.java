package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.apache.commons.collections4.SetUtils;
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

    private final ConcurrentMap<GraphLabel, Set<NodeIdInMemory>> labelsToNodes;

    private final ConcurrentMap<GraphNodeId, RelationshipsForNode> relationshipsForNodes;
    private final ConcurrentMap<NodeIdPair, EnumSet<TransportRelationshipTypes>> existingRelationships;
    private final RelationshipTypeCounts relationshipTypeCounts;
    private final boolean diagnostics;

    // todo proper transaction handling, rollbacks etc

    public Graph() {
        nextGraphNodeId = new AtomicInteger(0);
        nextRelationshipId = new AtomicInteger(0);

        nodesAndEdges = new NodesAndEdges();

        relationshipsForNodes = new ConcurrentHashMap<>();
        labelsToNodes = new ConcurrentHashMap<>();
        relationshipTypeCounts = new RelationshipTypeCounts();
        existingRelationships = new ConcurrentHashMap<>();

        // TODO into config and consolidate with neo4j diag option?
        diagnostics = true;
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        for(GraphLabel label : GraphLabel.values()) {
            labelsToNodes.put(label, new HashSet<>());
        }
        relationshipTypeCounts.reset();
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stop");

        synchronized (nodesAndEdges) {
            nextGraphNodeId.set(0);
            nextRelationshipId.set(0);

            nodesAndEdges.clear();
            relationshipsForNodes.clear();
            existingRelationships.clear();

            relationshipTypeCounts.reset();

            labelsToNodes.clear();
        }

        logger.info("stopped");
    }

    public static Graph createFrom(final NodesAndEdges incoming) {
        final Graph result = new Graph();
        result.start();

        loadNodes(incoming, result);
        loadRelationships(incoming, result);

        return result;
    }

    private static void loadRelationships(final NodesAndEdges incoming, final Graph target) {
        logger.info("Loading relationships");

        incoming.getRelationships().forEach(relationship -> {
            target.checkAndUpdateExistingRelationships(relationship.getType(), relationship.getStartId(), relationship.getEndId());
            target.captureRelationship(relationship.getType(), relationship, relationship.getStartId(), relationship.getEndId());
        });
        target.updateNextRelationshipId();
    }

    private static void loadNodes(final NodesAndEdges incoming, final Graph target) {
        logger.info("Loading nodes");
        incoming.getNodes().forEach(node -> {

            // add the node using id from the saved version
            final NodeIdInMemory id = node.getId();
            target.nodesAndEdges.addNode(id, node);

            // update labels for the node
            EnumSet<GraphLabel> labels = node.getLabels();
            labels.forEach(label -> target.labelsToNodes.get(label).add(id));
        });
        // using loaded id's work out new next node id
        target.updateNextNodeId();
        logger.info("Loaded nodes, new next node id is " + target.nextGraphNodeId.get());
    }

    private synchronized void updateNextNodeId() {
        nodesAndEdges.refreshNextNodeIdInto(nextGraphNodeId);
    }

    private synchronized void updateNextRelationshipId() {
        nodesAndEdges.captureNextRelationshipId(nextRelationshipId);
    }

    public GraphNodeInMemory createNode(final EnumSet<GraphLabel> labels) {
        synchronized (nodesAndEdges) {
            final int id = nextGraphNodeId.getAndIncrement();
            final NodeIdInMemory idInMemory;
            if (diagnostics) {
                idInMemory = new NodeIdInMemory(id, labels);
            } else {
                idInMemory = new NodeIdInMemory(id);
            }
            final GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(idInMemory, labels);
            final NodeIdInMemory graphNodeId = graphNodeInMemory.getId();
            nodesAndEdges.addNode(graphNodeId, graphNodeInMemory);
            labels.forEach(label -> labelsToNodes.get(label).add(graphNodeId));
            return graphNodeInMemory;
        }
    }

    public GraphRelationshipInMemory createRelationship(final TransportRelationshipTypes relationshipType,
                                                                     final GraphNodeInMemory begin,
                                                                     final GraphNodeInMemory end) {
        synchronized (nodesAndEdges) {
            final NodeIdInMemory beginId = begin.getId();
            final NodeIdInMemory endId = end.getId();

            checkAndUpdateExistingRelationships(relationshipType, beginId, endId);

            final int id = nextRelationshipId.getAndIncrement();
            final GraphRelationshipInMemory relationship = new GraphRelationshipInMemory(relationshipType,
                    new RelationshipIdInMemory(id), beginId, endId);

            captureRelationship(relationshipType, relationship, beginId, endId);
            return relationship;
        }
    }

    private void captureRelationship(final TransportRelationshipTypes relationshipType,
                                     final GraphRelationshipInMemory relationship,
                                     final NodeIdInMemory beginId, final NodeIdInMemory endId) {
        final RelationshipIdInMemory relationshipId = relationship.getId();

        nodesAndEdges.addRelationship(relationshipId, relationship);
        addOutboundTo(beginId, relationshipId);
        addInboundTo(endId, relationshipId);
        relationshipTypeCounts.increment(relationshipType);
    }

    private void checkAndUpdateExistingRelationships(final TransportRelationshipTypes relationshipType, final NodeIdInMemory beginId,
                                                     final NodeIdInMemory endId) {
        final NodeIdPair idPair = NodeIdPair.of(beginId, endId);
        if (existingRelationships.containsKey(idPair)) {
            if (existingRelationships.get(idPair).contains(relationshipType)) {
                String message = "Already have relationship of type " + relationshipType + " between " + beginId + " and " + endId;
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
        synchronized (relationshipsForNodes) {
            if (!relationshipsForNodes.containsKey(id)) {
                relationshipsForNodes.put(id, new RelationshipsForNode());
            }
            relationshipsForNodes.get(id).addInbound(relationshipId);
        }
    }

    private void addOutboundTo(final GraphNodeId id, final RelationshipIdInMemory relationshipId) {
        synchronized (relationshipsForNodes) {
            if (!relationshipsForNodes.containsKey(id)) {
                relationshipsForNodes.put(id, new RelationshipsForNode());
            }
            relationshipsForNodes.get(id).addOutbound(relationshipId);
        }
    }

    public Stream<GraphRelationshipInMemory> getRelationshipsFor(final NodeIdInMemory id, final GraphDirection direction) {
        synchronized (nodesAndEdges) {
            if (!nodesAndEdges.hasNode(id)) {
                String msg = "No such node " + id;
                logger.error(msg);
                throw new GraphException(msg);
            }
        }
        synchronized (relationshipsForNodes) {
            if (relationshipsForNodes.containsKey(id)) {
                final RelationshipsForNode relationshipsForNode = relationshipsForNodes.get(id);
                return switch (direction) {
                    case Outgoing -> nodesAndEdges.getOutbounds(relationshipsForNode);
                    case Incoming -> nodesAndEdges.getInbounds(relationshipsForNode);
                    case Both -> Stream.concat(nodesAndEdges.getOutbounds(relationshipsForNode),
                            nodesAndEdges.getInbounds(relationshipsForNode));
                };
            } else {
                logger.debug("node " + id + " has no relationships");
                return Stream.empty();
            }
        }
    }

    void delete(final RelationshipIdInMemory id) {
        synchronized (nodesAndEdges) {
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

            relationshipTypeCounts.decrement(relationship.getType());
        }
    }

    void delete(final NodeIdInMemory id) {
        synchronized (nodesAndEdges) {
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
    }

    private void deleteRelationshipFrom(final GraphNodeId graphNodeId, final RelationshipIdInMemory relationshipId) {
        if (relationshipsForNodes.containsKey(graphNodeId)) {
            RelationshipsForNode relationshipsForNode = relationshipsForNodes.get(graphNodeId);
            relationshipsForNode.remove(relationshipId);
        }
    }

    public GraphNodeInMemory getNode(final NodeIdInMemory nodeId) {
        synchronized (nodesAndEdges) {
            if (!nodesAndEdges.hasNode(nodeId)) {
                String msg = "No such node " + nodeId;
                logger.error(msg);
                throw new GraphException(msg);
            }
            return nodesAndEdges.getNode(nodeId);
        }
    }

    public Stream<GraphNodeInMemory> findNodes(final GraphLabel graphLabel) {
        synchronized (nodesAndEdges) {
            final Set<NodeIdInMemory> matchingIds = labelsToNodes.get(graphLabel);
            return matchingIds.stream().map(nodesAndEdges::getNode);
        }
    }

    GraphRelationship getRelationship(final RelationshipIdInMemory graphRelationshipId) {
        synchronized (nodesAndEdges) {
            if (nodesAndEdges.hasRelationship(graphRelationshipId)) {
                return nodesAndEdges.getRelationship(graphRelationshipId);
            } else {
                String msg = "No such relationship " + graphRelationshipId;
                logger.error(msg);
                throw new GraphException(msg);
            }
        }
    }

    void addLabel(final NodeIdInMemory id, final GraphLabel label) {
        synchronized (nodesAndEdges) {
            labelsToNodes.get(label).add(id);
        }
    }

    @JsonIgnore
    public long getNumberOf(TransportRelationshipTypes relationshipType) {
        return relationshipTypeCounts.get(relationshipType);
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

    @Override
    public String toString() {
        return "Graph{" +
                "nextGraphNodeId=" + nextGraphNodeId +
                ", nextRelationshipId=" + nextRelationshipId +
                ", nodesAndEdges=" + nodesAndEdges +
                ", labelsToNodes=" + labelsToNodes.size() +
                ", relationshipsForNodes=" + relationshipsForNodes.size() +
                ", existingRelationships=" + existingRelationships.size() +
                ", relationshipTypeCounts=" + relationshipTypeCounts +
                '}';
    }

    public static boolean same(final Graph a, final Graph b) {
        if (a.nextRelationshipId.get()!=b.nextRelationshipId.get()) {
            logger.error("check same nextRelationshipId" + a.nextRelationshipId + "!=" + b.nextRelationshipId);
            return false;
        }
        if (a.nextGraphNodeId.get()!=b.nextGraphNodeId.get()) {
            logger.error("check same nextGraphNodeId" + a.nextGraphNodeId + "!=" + b.nextGraphNodeId);
            return false;
        }
        if (!a.nodesAndEdges.equals(b.nodesAndEdges)) {
            logger.error("check same nodesAndEdges" + a.nodesAndEdges + "!=" + b.nodesAndEdges);
        }
        if (!same(a.relationshipsForNodes, b.relationshipsForNodes)) {
            return false;
        }
        if (!same(a.existingRelationships, b.existingRelationships)) {
            return false;
        }
        if (!(a.relationshipTypeCounts.equals(b.relationshipTypeCounts))) {
            return false;
        }

        return true;
    }

    private static boolean same(Map<?, ?> a, Map<?, ?> b) {
        final Set<Map.Entry<?, ?>> diffs = SetUtils.disjunction(a.entrySet(), b.entrySet());
        if (diffs.isEmpty()) {
            return true;
        }
        logger.error("Diffs " + diffs);
        return false;
    }

    private static class NodeIdPair {
        private final GraphNodeId first;
        private final GraphNodeId second;

        public static NodeIdPair of(NodeIdInMemory begin, NodeIdInMemory end) {
            return new NodeIdPair(begin, end);
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

    private static class RelationshipTypeCounts {
        ConcurrentHashMap<TransportRelationshipTypes, Integer> theMap;

        RelationshipTypeCounts() {
            theMap = new ConcurrentHashMap<>();
        }

        public void reset() {
            for(TransportRelationshipTypes type : TransportRelationshipTypes.values()) {
                theMap.put(type, 0);
            }
        }

        public void increment(TransportRelationshipTypes relationshipType) {
            theMap.compute(relationshipType, (k, current) -> current + 1);
        }

        public synchronized void decrement(TransportRelationshipTypes relationshipType) {
            theMap.compute(relationshipType, (k, current) -> current - 1);
        }

        public synchronized int get(TransportRelationshipTypes relationshipType) {
            return theMap.get(relationshipType);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            RelationshipTypeCounts that = (RelationshipTypeCounts) o;
            return Objects.equals(theMap, that.theMap);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(theMap);
        }
    }
}
