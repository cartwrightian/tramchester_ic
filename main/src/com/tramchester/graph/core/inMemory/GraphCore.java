package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import jakarta.inject.Inject;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

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

    private final RelationshipsForNodes relationshipsForNodes;
    private final ConcurrentMap<NodeIdPair, EnumSet<TransportRelationshipTypes>> relationshipTypesBetweenNodes;
    private final RelationshipTypeCounts relationshipTypeCounts;
    private final boolean diagnostics;
    private final boolean local; // aka scoped to one transaction

    // todo proper transaction handling, rollbacks etc
    // TODO need way to know this is the 'core' graph, not a local copy??

    /***
     * Use with care, normally want the local copy version of this cons
     * @param idFactory global id factory
     */
    @Inject
    private GraphCore(final GraphIdFactory idFactory) {
        this(idFactory, false);
    }

    public GraphCore(final GraphIdFactory idFactory, final boolean local) {
        this.idFactory = idFactory;

        nodesAndEdges = new NodesAndEdges();

        relationshipsForNodes = new RelationshipsForNodes();
        labelsToNodes = new ConcurrentHashMap<>();
        relationshipTypeCounts = new RelationshipTypeCounts();
        relationshipTypesBetweenNodes = new ConcurrentHashMap<>();

        // TODO into config and consolidate with neo4j diag option?
        diagnostics = false;

        this.local = local;
    }


    @PostConstruct
    private void start() {
        doStart(false); // the global scope
    }

    public void doStart(final boolean local) {
        if (this.local!=local) {
            throw new RuntimeException("Scoping issue, called with local=" + local + " but created as " + this.local);
        }
        final String postfix = local ? "local" : "global";
        final LoggingEventBuilder level = local ? logger.atDebug() : logger.atInfo();

        level.log("Starting " + postfix);
        for(GraphLabel label : GraphLabel.values()) {
            labelsToNodes.put(label, new HashSet<>());
        }
        relationshipTypeCounts.reset();
        level.log("started " + postfix);
    }

    @PreDestroy
    public void stop() {
        doStop(false);
    }

    protected void doStop(final boolean local) {
        if (this.local!=local) {
            throw new RuntimeException("Scoping issue, called with local=" + local + " but created as " + this.local);
        }

        final String postfix = local ? "local" : "global";
        final LoggingEventBuilder level = local ? logger.atDebug() : logger.atInfo();

        level.log("stop " + postfix);

        if (diagnostics && !local) {
            nodesAndEdges.logUnusedProperties(logger);
        }

        synchronized (nodesAndEdges) {

            nodesAndEdges.clear();
            relationshipsForNodes.clear();
            relationshipTypesBetweenNodes.clear();

            relationshipTypeCounts.reset();

            labelsToNodes.clear();
        }

        level.log("stopped " + postfix);
    }

    // supports load from file
    public static GraphCore createFrom(final NodesAndEdges incoming, final GraphIdFactory graphIdFactory) {
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
            final ImmutableEnumSet<GraphLabel> labels = node.getLabels();
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
    public GraphNodeInMemory createNode(final ImmutableEnumSet<GraphLabel> labels) {
        synchronized (nodesAndEdges) {
            final int id = idFactory.getNextNodeId();
            final NodeIdInMemory idInMemory;
            if (diagnostics) {
                idInMemory = new NodeIdInMemory(id, labels);
            } else {
                idInMemory = new NodeIdInMemory(id);
            }
            final GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(idInMemory, labels, diagnostics);
            return insertNode(graphNodeInMemory, labels);
        }
    }

    synchronized GraphNodeInMemory insertNode(final GraphNodeInMemory nodeToInsert, final ImmutableEnumSet<GraphLabel> labels) {
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

            final int id = idFactory.getNextRelationshipId();
            final GraphRelationshipInMemory relationship = new GraphRelationshipInMemory(relationshipType,
                    new RelationshipIdInMemory(id), beginId, endId, diagnostics);

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
        final NodeIdPair key = NodeIdPair.of(beginId, endId);
        if (!relationshipTypesBetweenNodes.containsKey(key)) {
            relationshipTypesBetweenNodes.put(key, EnumSet.noneOf(TransportRelationshipTypes.class));
        }
        relationshipTypesBetweenNodes.get(key).add(relationshipType);
        return relationship;
    }

    private void checkAndUpdateExistingRelationships(final TransportRelationshipTypes relationshipType, final NodeIdInMemory beginId,
                                                     final NodeIdInMemory endId) {
        final NodeIdPair idPair = NodeIdPair.of(beginId, endId);

        if (relationshipTypesBetweenNodes.containsKey(idPair)) {
            if (relationshipTypesBetweenNodes.get(idPair).contains(relationshipType)) {
                // can only have one of the specified type
                String message = "Already have relationship of type " + relationshipType + " between " + beginId + " and " + endId;
                logger.error(message);
                throw new RuntimeException(message);
            } else {
                // record we have a relationship of this type between begin and end nodes
                relationshipTypesBetweenNodes.get(idPair).add(relationshipType);
            }
        } else {
            // no relationships for this node yet
            relationshipTypesBetweenNodes.put(idPair, EnumSet.of(relationshipType));
        }

    }

    /***
     * Add an inbound for node nodeId
     * @param nodeId parent node id
     * @param relationshipId id of outbound relationship to add
     * @return true if added a new relationship
     */
    private boolean putInboundTo(final GraphNodeId nodeId, final RelationshipIdInMemory relationshipId) {
        return relationshipsForNodes.addInboundFor(nodeId, relationshipId);
    }

    /***
     * Add an outbound for node nodeId
     * @param nodeId parent node id
     * @param relationshipId id of outbound relationship to add
     * @return true if added a new relationship
     */
    private boolean putOutboundTo(final GraphNodeId nodeId, final RelationshipIdInMemory relationshipId) {
        return relationshipsForNodes.addOutboundFor(nodeId, relationshipId);
    }

    @Override
    public Stream<GraphRelationship> findRelationshipsImmutableFor(final NodeIdInMemory id, final GraphDirection direction) {
        return findRelationshipsMutableFor(id, direction).map(item -> item);
    }

    @Override
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(final NodeIdInMemory id, final GraphDirection direction) {
        final RelationshipsForNode relationshipsForNode = relationshipsForNodes.get(id);
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
    public void delete(final RelationshipIdInMemory relId) {
        synchronized (nodesAndEdges) {
            if (!nodesAndEdges.hasRelationship(relId)) {
                String msg = "Cannot delete relationship, missing id " + relId;
                logger.error(msg);
                throw new GraphException(msg);
            }
            final GraphRelationshipInMemory relationship = nodesAndEdges.getRelationship(relId);
            final GraphNodeId begin = relationship.getStartId();
            final GraphNodeId end = relationship.getEndId();

            deleteRelationshipFrom(begin, relId);
            deleteRelationshipFrom(end, relId);

            nodesAndEdges.removeRelationship(relId);

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
            relationshipsForNodes.remove(id);
            // label map
            final ImmutableEnumSet<GraphLabel> labels = nodesAndEdges.getNode(id).getLabels();
            labels.forEach(label -> labelsToNodes.get(label).remove(id));
            // the node
            nodesAndEdges.removeNode(id);
        }
    }

    private void deleteRelationshipFrom(final GraphNodeId graphNodeId, final RelationshipIdInMemory relationshipId) {
        relationshipsForNodes.removeFrom(graphNodeId, relationshipId);
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
    public Stream<GraphRelationship> findRelationships(final TransportRelationshipTypes type) {
        return nodesAndEdges.getRelationships().stream().
                filter(rel -> rel.isType(type)).
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

    @Override
    public Stream<GraphNodeInMemory> getUpdatedNodes() {
        throw new RuntimeException("Not implemented for core");
    }

    @Override
    public Stream<GraphRelationshipInMemory> getUpdatedRelationships() {
        throw new RuntimeException("Not implemented for core");
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
                Objects.equals(relationshipTypesBetweenNodes, graph.relationshipTypesBetweenNodes) &&
                Objects.equals(labelsToNodes, graph.labelsToNodes) &&
                Objects.equals(relationshipTypeCounts, graph.relationshipTypeCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodesAndEdges, relationshipsForNodes, relationshipTypesBetweenNodes, labelsToNodes, relationshipTypeCounts);
    }

    @Override
    public String toString() {
        return "Graph{" +
                ", nodesAndEdges=" + nodesAndEdges +
                ", labelsToNodes=" + labelsToNodes.size() +
                ", relationshipsForNodes=" + relationshipsForNodes.size() +
                ", relationshipTypesBetweenNodes=" + relationshipTypesBetweenNodes.size() +
                ", relationshipTypeCounts=" + relationshipTypeCounts +
                '}';
    }

    /***
     * Test support
     * @param a
     * @param b
     * @return true if same
     */
    public static boolean same(final GraphCore a, final GraphCore b) {
        if (a.local != b.local) {
            throw new RuntimeException("Mismatch on scoping a.local = " + a.local + " and b.local=" + b.local);
        }

        if (!GraphIdFactory.same(a.idFactory, b.idFactory)) {
            logger.error("check same idFactory" + a.idFactory + "!=" + b.idFactory);
            return false;
        }
        if (!(a.relationshipTypeCounts.equals(b.relationshipTypeCounts))) {
            logger.error("check same relationship type counts " + a.relationshipTypeCounts + "!=" + b.relationshipTypeCounts);
            return false;
        }
        if (!a.nodesAndEdges.equals(b.nodesAndEdges)) {
            logger.error("check same nodesAndEdges" + a.nodesAndEdges + "!=" + b.nodesAndEdges);
            return false;
        }
        if (!same(a.relationshipsForNodes.theMap, b.relationshipsForNodes.theMap)) {
            logger.error("check same relationships for nodes " + a.relationshipsForNodes + "!=" + b.relationshipsForNodes);
            return false;
        }
        if (!same(a.relationshipTypesBetweenNodes, b.relationshipTypesBetweenNodes)) {
            logger.error("check same relationship types " + a.relationshipTypesBetweenNodes + "!=" + b.relationshipTypesBetweenNodes);
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

    public void commitChanges(final Graph mutatedGraph, final Set<RelationshipIdInMemory> relationshipsToDelete,
                              final Set<NodeIdInMemory> nodesToDelete) {
        if (this.local) {
            String message = "Commit against a local copy!";
            logger.error(message);
            throw new RuntimeException(message);
        }

        synchronized (nodesAndEdges) {
            for (RelationshipIdInMemory relationshipIdInMemory : relationshipsToDelete) {
                delete(relationshipIdInMemory);
            }
            for (NodeIdInMemory nodeIdInMemory : nodesToDelete) {
                delete(nodeIdInMemory);
            }

            final Stream<GraphNodeInMemory> nodesFromChild = mutatedGraph.getUpdatedNodes();
            nodesFromChild.forEach(node -> {
                insertNode(node, node.getLabels());
                node.setClean();
            });

            final Stream<GraphRelationshipInMemory> relationshipsFromChild = mutatedGraph.getUpdatedRelationships();
            relationshipsFromChild.forEach(relationship -> {
                insertRelationship(relationship.getType(), relationship, relationship.getStartId(), relationship.getEndId());
                relationship.setClean();
            });
        }

    }

    Stream<RelationshipIdInMemory> getRelationshipsIdsFor(final NodeIdInMemory nodeId) {
        return relationshipsForNodes.getRelationshipIdsFor(nodeId);
    }

    public ImmutableEnumSet<TransportRelationshipTypes> getTypesBetween(final GraphNodeId idA, final GraphNodeId idB) {
        final NodeIdPair key = NodeIdPair.of((NodeIdInMemory) idA, (NodeIdInMemory) idB);
        if (relationshipTypesBetweenNodes.containsKey(key)) {
            return ImmutableEnumSet.copyOf(relationshipTypesBetweenNodes.get(key));
        } else {
            return ImmutableEnumSet.noneOf(TransportRelationshipTypes.class);
        }
    }

    private record NodeIdPair(GraphNodeId first, GraphNodeId second) {
            public static NodeIdPair of(NodeIdInMemory begin, NodeIdInMemory end) {
                return new NodeIdPair(begin, end);
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
            // can't compare directly when using Atomic Ints
            boolean keysSame = theMap.keySet().equals(that.theMap.keySet());
            if (keysSame) {
                for (TransportRelationshipTypes relationshipType : theMap.keySet()) {
                    if (theMap.get(relationshipType).get()!=that.theMap.get(relationshipType).get()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(theMap);
        }

        @Override
        public String toString() {
            return "RelationshipTypeCounts{" +
                    "theMap=" + theMap +
                    '}';
        }
    }

    private static class RelationshipsForNodes {
        private final ConcurrentMap<GraphNodeId, RelationshipsForNode> theMap;

        private RelationshipsForNodes() {
            theMap = new ConcurrentHashMap<>();
        }

        public synchronized void clear() {
            theMap.clear();
        }

        public synchronized Stream<RelationshipIdInMemory> getRelationshipIdsFor(final NodeIdInMemory nodeId) {
            return theMap.getOrDefault(nodeId, RelationshipsForNode.empty()).getRelationshipIds();
        }

        public synchronized boolean addInboundFor(final GraphNodeId nodeId, final RelationshipIdInMemory relationshipId) {
            final RelationshipsForNode relationshipsForNode = theMap.computeIfAbsent(nodeId, key -> new RelationshipsForNode());
            return relationshipsForNode.putInbound(relationshipId);
        }

        public synchronized boolean addOutboundFor(final GraphNodeId nodeId, final RelationshipIdInMemory relationshipId) {
            final RelationshipsForNode relationshipsForNode = theMap.computeIfAbsent(nodeId, key -> new RelationshipsForNode());
            return relationshipsForNode.putOutbound(relationshipId);
        }

        public synchronized RelationshipsForNode get(final NodeIdInMemory id) {
            return theMap.getOrDefault(id, RelationshipsForNode.empty());
        }

        public synchronized void remove(final NodeIdInMemory id) {
            if (theMap.containsKey(id)) {
                final RelationshipsForNode forNode = theMap.get(id);
                if (!forNode.isEmpty()) {
                    String msg = "Node " + id + " still has relationships " + forNode;
                    logger.error(msg);
                    throw new GraphException(msg);
                }
                theMap.remove(id);
            }
        }

        public synchronized void removeFrom(final GraphNodeId graphNodeId, final RelationshipIdInMemory relationshipId) {
            if (theMap.containsKey(graphNodeId)) {
                final RelationshipsForNode relationshipsForNode = theMap.get(graphNodeId);
                relationshipsForNode.remove(relationshipId);
            }
        }

        public long size() {
            return theMap.size();
        }
    }
}
