package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.*;
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
import java.util.concurrent.atomic.AtomicInteger;
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
    // TODO need way to know this is the 'core' graph, not a local copy??

    /***
     * Use with care, normally want the local copy version of this cons
     * @param idFactory global id factory
     */
    @Inject
    public GraphCore(final GraphIdFactory idFactory) {
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
            target.insertRelationship(relationship.getType(), relationship, relationship.getStartId(), relationship.getEndId());
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
//            final NodeIdInMemory graphNodeId = graphNodeInMemory.getId();
//            nodesAndEdges.addNode(graphNodeId, graphNodeInMemory);
//            labels.forEach(label -> labelsToNodes.get(label).add(graphNodeId));
            return insertNode(graphNodeInMemory, labels);
            //return graphNodeInMemory;
        }
    }

    synchronized GraphNodeInMemory insertNode(final GraphNodeInMemory nodeToInsert, final EnumSet<GraphLabel> labels) {
        final NodeIdInMemory id = nodeToInsert.getId();
        nodesAndEdges.addNode(id, nodeToInsert);
        labels.forEach(label -> labelsToNodes.get(label).add(id));
        return nodeToInsert;
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

            insertRelationship(relationshipType, relationship, beginId, endId);
            return relationship;
        }
    }

    GraphRelationshipInMemory insertRelationship(final TransportRelationshipTypes relationshipType,
                                    final GraphRelationshipInMemory relationship,
                                    final NodeIdInMemory beginId, final NodeIdInMemory endId) {

        final RelationshipIdInMemory relationshipId = relationship.getId();

        // TODO sanity check direction and type have not changed? These are immutable properties for the relationship
        nodesAndEdges.putRelationship(relationshipId, relationship);

        final boolean addInbound = putInboundTo(endId, relationshipId);
        final boolean addOutbound = putOutboundTo(beginId, relationshipId);
        if (addInbound||addOutbound) {
            relationshipTypeCounts.increment(relationshipType);
        }
        return relationship;
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

    /***
     * Add an inbound for node nodeId
     * @param nodeId parent node id
     * @param relationshipId id of outbound relationship to add
     * @return true if added a new relationship
     */
    private boolean putInboundTo(final GraphNodeId nodeId, final RelationshipIdInMemory relationshipId) {
        synchronized (relationshipsForNodes) {
            if (!relationshipsForNodes.containsKey(nodeId)) {
                relationshipsForNodes.put(nodeId, new RelationshipsForNode());
            }
            return relationshipsForNodes.get(nodeId).putInbound(relationshipId);
        }
    }

    /***
     * Add an outbound for node nodeId
     * @param nodeId parent node id
     * @param relationshipId id of outbound relationship to add
     * @return true if added a new relationship
     */
    private boolean putOutboundTo(final GraphNodeId nodeId, final RelationshipIdInMemory relationshipId) {
        synchronized (relationshipsForNodes) {
            if (!relationshipsForNodes.containsKey(nodeId)) {
                relationshipsForNodes.put(nodeId, new RelationshipsForNode());
            }
            return relationshipsForNodes.get(nodeId).putOutbound(relationshipId);
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

    /***
     * Find single relationship
     * If one found returns that
     * If none found return null
     * If >1 found throws exception
     * @param id the node id
     * @param direction Incoming or Outgoing
     * @param transportRelationshipType type of relationship
     * @return the single relationship that matched
     */
    @Override
    public GraphRelationshipInMemory getSingleRelationshipMutable(final NodeIdInMemory id, final GraphDirection direction,
                                                                  final TransportRelationshipTypes transportRelationshipType) {
        final List<GraphRelationshipInMemory> result = findRelationshipsMutableFor(id, direction).
                filter(rel -> rel.isType(transportRelationshipType)).
                toList();

        if (result.isEmpty()) {
            return null;
        }
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
    public GraphRelationshipInMemory getRelationship(final RelationshipIdInMemory graphRelationshipId) {
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

    @Override
    public void commit(final GraphTransaction owningTransaction) {
        throw new RuntimeException("Unexpected commit for " + owningTransaction);
    }

    @Override
    public void close(final GraphTransaction owningTransaction) {
        throw new RuntimeException("Unexpected close for " + owningTransaction);
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

    public void commitChanges(GraphCore childGraph, Set<RelationshipIdInMemory> relationshipsToDelete, Set<NodeIdInMemory> nodesToDelete) {
        synchronized (nodesAndEdges) {
            for (RelationshipIdInMemory relationshipIdInMemory : relationshipsToDelete) {
                delete(relationshipIdInMemory);
            }
            for (NodeIdInMemory nodeIdInMemory : nodesToDelete) {
                delete(nodeIdInMemory);
            }
            // TODO Use Dirty flag here
            final Set<GraphNodeInMemory> nodesFromChild = childGraph.nodesAndEdges.getNodes();
            for (GraphNodeInMemory graphNodeInMemory : nodesFromChild) {
                insertNode(graphNodeInMemory, graphNodeInMemory.getLabels());
            }
            // TODO Use Dirty flag here
            final Set<GraphRelationshipInMemory> relationshipsFromChild = childGraph.nodesAndEdges.getRelationships();
            for (GraphRelationshipInMemory relationship : relationshipsFromChild) {
                insertRelationship(relationship.getType(), relationship, relationship.getStartId(), relationship.getEndId());
            }
        }

        //throw new RuntimeException("TODO");
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

    /***
     * Cached, performance
     */
    private static class RelationshipTypeCounts {
        ConcurrentHashMap<TransportRelationshipTypes, AtomicInteger> theMap;

        RelationshipTypeCounts() {
            theMap = new ConcurrentHashMap<>();
            reset();
        }

        public void reset() {
            for(TransportRelationshipTypes type : TransportRelationshipTypes.values()) {
                theMap.put(type, new AtomicInteger(0));
            }
        }

        public void increment(final TransportRelationshipTypes relationshipType) {
            theMap.get(relationshipType).getAndIncrement();
        }

        public synchronized void decrement(final TransportRelationshipTypes relationshipType) {
            theMap.get(relationshipType).getAndDecrement();
        }

        public synchronized int get(final TransportRelationshipTypes relationshipType) {
            return theMap.get(relationshipType).get();
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
