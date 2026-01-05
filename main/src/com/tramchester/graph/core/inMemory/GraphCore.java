package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import jakarta.inject.Inject;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@JsonPropertyOrder({"nodes", "relationships"})
@LazySingleton
public class GraphCore implements Graph {
    private static final Logger logger = LoggerFactory.getLogger(GraphCore.class);

    private final GraphIdFactory idFactory;

    private final NodesAndEdges nodesAndEdges;

    private final ConcurrentMap<GraphLabel, Set<NodeIdInMemory>> labelsToNodes;

    private final ConcurrentMap<GraphNodeId, RelationshipsForNode> relationshipsForNodes;
    private final ConcurrentMap<NodeIdPair, EnumSet<TransportRelationshipTypes>> existingRelationships;
    private final RelationshipTypeCounts relationshipTypeCounts;
    private final boolean diagnostics;

    // todo proper transaction handling, rollbacks etc

    @Inject
    public GraphCore(GraphIdFactory idFactory) {
        this.idFactory = idFactory;

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
//            nextGraphNodeId.set(0);
//            nextRelationshipId.set(0);

            nodesAndEdges.clear();
            relationshipsForNodes.clear();
            existingRelationships.clear();

            relationshipTypeCounts.reset();

            labelsToNodes.clear();
        }

        logger.info("stopped");
    }

    static GraphCore createFrom(final NodesAndEdges incoming, GraphIdFactory graphIdFactory) {
        final GraphCore result = new GraphCore(graphIdFactory);
        result.start();

        loadNodes(incoming, result);
        loadRelationships(incoming, result);

        return result;
    }

    private static void loadRelationships(final NodesAndEdges incoming, final GraphCore target) {
        logger.info("Loading relationships");

        incoming.getRelationships().forEach(relationship -> {
            target.checkAndUpdateExistingRelationships(relationship.getType(), relationship.getStartId(), relationship.getEndId());
            target.captureRelationship(relationship.getType(), relationship, relationship.getStartId(), relationship.getEndId());
        });
        target.updateNextRelationshipId();
    }

    private static void loadNodes(final NodesAndEdges incoming, final GraphCore target) {
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
        logger.info("Loaded nodes");
    }

    private synchronized void updateNextNodeId() {
        nodesAndEdges.refreshNextNodeIdInto(idFactory);
    }

    private synchronized void updateNextRelationshipId() {
        nodesAndEdges.captureNextRelationshipId(idFactory);
    }

    @Override
    public GraphNodeInMemory createNode(final EnumSet<GraphLabel> labels) {
        synchronized (nodesAndEdges) {
            final int id = idFactory.getNextNodeId(); //nextGraphNodeId.getAndIncrement();
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

    @Override
    public GraphRelationshipInMemory createRelationship(final TransportRelationshipTypes relationshipType,
                                                        final GraphNodeInMemory begin,
                                                        final GraphNodeInMemory end) {
        synchronized (nodesAndEdges) {
            final NodeIdInMemory beginId = begin.getId();
            final NodeIdInMemory endId = end.getId();

            checkAndUpdateExistingRelationships(relationshipType, beginId, endId);

            final int id = idFactory.getNextRelationshipId(); //nextRelationshipId.getAndIncrement();
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

    @Override
    public Stream<GraphRelationship> findRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction) {
        return findRelationshipsMutableFor(id, direction).map(item -> item);
    }

    @Override
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(final NodeIdInMemory id, final GraphDirection direction) {
        final RelationshipsForNode relationshipsForNode = relationshipsForNodes.getOrDefault(id, RelationshipsForNode.empty());
        return switch (direction) {
                    case Outgoing -> nodesAndEdges.getOutbounds(relationshipsForNode);
                    case Incoming -> nodesAndEdges.getInbounds(relationshipsForNode);
                    case Both -> Stream.concat(nodesAndEdges.getOutbounds(relationshipsForNode),
                            nodesAndEdges.getInbounds(relationshipsForNode));
                };
    }

    @Override
    public GraphRelationshipInMemory getSingleRelationshipMutable(final NodeIdInMemory id, final GraphDirection direction,
                                                                  final TransportRelationshipTypes transportRelationshipType) {
        final List<GraphRelationshipInMemory> result = findRelationshipsMutableFor(id, direction).
                filter(rel -> rel.isType(transportRelationshipType)).
                toList();

        if (result.size()==1) {
            return result.getFirst();
        }
        String msg = "Wrong number of relationships " + result.size();
        logger.error(msg);
        throw new GraphException(msg);
    }

    @Override
    public void delete(final RelationshipIdInMemory id) {
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

    @Override
    public void delete(final NodeIdInMemory id) {
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
                relationshipsForNodes.remove(id);
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

    @Override
    public GraphNodeInMemory getNodeMutable(final NodeIdInMemory nodeId) {
        return nodesAndEdges.getNode(nodeId);
    }

    @Override
    public GraphNode getNodeImmutable(final NodeIdInMemory nodeId) {
        return nodesAndEdges.getNode(nodeId);
    }

    @Override
    public Stream<GraphNodeInMemory> findNodesMutable(final GraphLabel graphLabel) {
        synchronized (nodesAndEdges) {
            final Set<NodeIdInMemory> matchingIds = labelsToNodes.get(graphLabel);
            return matchingIds.stream().map(nodesAndEdges::getNode);
        }
    }

    @Override
    public Stream<GraphNode> findNodesImmutable(final GraphLabel graphLabel) {
        return findNodesMutable(graphLabel).map(item -> item);
    }

    @Override
    public Stream<GraphNode> findNodesImmutable(GraphLabel label, GraphPropertyKey key, String value) {
        return findNodesMutable(label).
                filter(node -> node.hasProperty(key)).
                filter(node -> node.getProperty(key).equals(value)).
                map(item -> item);
    }

    @Override
    public GraphRelationship getRelationship(final RelationshipIdInMemory graphRelationshipId) {
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

    @Override
    public void addLabel(final NodeIdInMemory id, final GraphLabel label) {
        synchronized (nodesAndEdges) {
            labelsToNodes.get(label).add(id);
        }
    }

    @JsonIgnore
    public long getNumberOf(TransportRelationshipTypes relationshipType) {
        return relationshipTypeCounts.get(relationshipType);
    }

    /***
     * Save and Test support
     *
     * @return Nodes and Edges support
     */
    public NodesAndEdges getNodesAndEdges() {
        return nodesAndEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphCore graph = (GraphCore) o;
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
                ", nodesAndEdges=" + nodesAndEdges +
                ", labelsToNodes=" + labelsToNodes.size() +
                ", relationshipsForNodes=" + relationshipsForNodes.size() +
                ", existingRelationships=" + existingRelationships.size() +
                ", relationshipTypeCounts=" + relationshipTypeCounts +
                '}';
    }

    public static boolean same(final GraphCore a, final GraphCore b) {
        if (!GraphIdFactory.same(a.idFactory, b.idFactory)) {
            logger.error("check same idFactory" + a.idFactory + "!=" + b.idFactory);
            return false;
        }
//        if (a.nextRelationshipId.get()!=b.nextRelationshipId.get()) {
//            logger.error("check same nextRelationshipId" + a.nextRelationshipId + "!=" + b.nextRelationshipId);
//            return false;
//        }
//        if (a.nextGraphNodeId.get()!=b.nextGraphNodeId.get()) {
//            logger.error("check same nextGraphNodeId" + a.nextGraphNodeId + "!=" + b.nextGraphNodeId);
//            return false;
//        }
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

    public boolean hasNodeId(NodeIdInMemory nodeId) {
        return nodesAndEdges.hasNode(nodeId);
    }

    public boolean hasRelationshipId(RelationshipIdInMemory graphRelationshipId) {
        return nodesAndEdges.hasRelationship(graphRelationshipId);
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
