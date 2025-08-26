package com.tramchester.graph.core.neo4j;

import com.google.common.collect.Streams;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.caches.SharedRelationshipCache;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Stream;

/***
 * Facade around underlying graph DB Transaction
 */
public class MutableGraphTransactionNeo4J implements GraphTransactionNeo4J, MutableGraphTransaction {
    private final Transaction txn;
    private final GraphIdFactory idFactory;
    private final GraphReferenceMapper relationshipTypeFactory;
    private final TransactionObserver transactionObserver;
    private final int transactionId;

    /*
     these cache attributes of the nodes, not the nodes themselves as these cannot be accessed outside of the
     parent transaction
    */
    private final SharedNodeCache sharedNodeCache;
    private final SharedRelationshipCache sharedRelationshipCache;

    /* GraphIdFactory ownership is passed in */
    protected MutableGraphTransactionNeo4J(final Transaction txn, final GraphIdFactory idFactory, GraphReferenceMapper graphReferenceMapper, final int transactionId,
                                           final TransactionObserver transactionObserver, SharedNodeCache sharedNodeCache,
                                           SharedRelationshipCache sharedRelationshipCache) {
        this.txn = txn;
        this.idFactory = idFactory;
        this.relationshipTypeFactory = graphReferenceMapper;
        this.transactionId = transactionId;
        this.transactionObserver = transactionObserver;
        this.sharedNodeCache = sharedNodeCache;
        this.sharedRelationshipCache = sharedRelationshipCache;
    }

    @Override
    public MutableGraphRelationship createRelationship(MutableGraphNode begin, MutableGraphNode end, TransportRelationshipTypes relationshipType) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getTransactionId() {
        return transactionId;
    }

    @Override
    public void close() {
        txn.close();
        transactionObserver.onClose(this);
        idFactory.close();
    }

    public void commit() {
        txn.commit();
        transactionObserver.onCommit(this);
    }

    @Override
    public MutableGraphNode createNode(final GraphLabel graphLabel) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        final Node node = txn.createNode(label);
        return wrapNodeAsMutable(node);
    }

    @Override
    public MutableGraphNode createNode(final EnumSet<GraphLabel> labels) {
        final Label[] toApply = relationshipTypeFactory.getLabels(labels);
        //labels.toArray(toApply);
        final Node node = txn.createNode(toApply);
        return wrapNodeAsMutable(node);
    }

    public DBSchema schema() {
        return new DBSchema(txn.schema());
    }

    @Override
    public GraphNode getNodeById(final GraphNodeId nodeId) {
        final Node node = getNode(nodeId);
        return wrapNodeAsImmutable(node);
    }

    @Override
    public MutableGraphNode getNodeByIdMutable(final GraphNodeId nodeId) {
        final Node node = getNode(nodeId);
        return wrapNodeAsMutable(node);
    }

    private Node getNode(GraphNodeId nodeId) {
        GraphNodeIdNeo4J actualNodeId = (GraphNodeIdNeo4J) nodeId;
        return actualNodeId.getNodeFrom(txn);
    }

    @Override
    public ImmutableGraphRelationshipNeo4J getRelationshipById(final GraphRelationshipId graphRelationshipId) {
        final Relationship relationship = getRelationship(graphRelationshipId);
        if (relationship==null) {
            return null;
        }
        return wrapRelationship(relationship);
    }

    private Relationship getRelationship(GraphRelationshipId graphRelationshipId) {
        GraphRelationshipIdNeo4J actualId = (GraphRelationshipIdNeo4J) graphRelationshipId;
        return actualId.getRelationshipFrom(txn);
    }

    @Override
    public Stream<GraphNode> findNodes(final GraphLabel graphLabel) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        return txn.findNodes(label).stream().map(this::wrapNodeAsImmutable);
    }

    @Override
    public Stream<GraphRelationship> findRelationships(final TransportRelationshipTypes relationshipType) {
        final RelationshipType actual = relationshipTypeFactory.get(relationshipType);
        final ResourceIterator<Relationship> found = txn.findRelationships(actual);
        return found.stream().map(this::wrapRelationship);
    }

    @Override
    public Stream<MutableGraphNode> findNodesMutable(GraphLabel graphLabel) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        return txn.findNodes(label).stream().map(this::wrapNodeAsMutable);
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel graphLabel, final GraphPropertyKey key, final String value) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        final Node node = txn.findNode(label, key.getText(), value);
        return node != null;
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel graphLabel) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        final ResourceIterator<Node> found = txn.findNodes(label);
        final List<Node> nodes = found.stream().toList();
        return !nodes.isEmpty();
    }

    private MutableGraphNodeNeo4J findNodeMutable(final GraphLabel label, final GraphPropertyKey key, final String value) {
        return findNodeMutable(label, key.getText(), value);
    }

    private GraphNode findNode(final GraphLabel label, final GraphPropertyKey key, final String value) {
        return findNode(label, key.getText(), value);
    }

    private GraphNode findNode(final GraphLabel graphLabel, final String key, final String value) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        final Node node = txn.findNode(label, key, value);
        if (node==null) {
            return null;
        }
        return wrapNodeAsImmutable(node);
    }

    private MutableGraphNodeNeo4J findNodeMutable(final GraphLabel graphLabel, final String key, final String value) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        final Node node = txn.findNode(label, key, value);
        if (node==null) {
            return null;
        }
        return wrapNodeAsMutable(node);
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(final ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(final ITEM item) {
        return findNodeMutable(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    // TODO strong typed version so make sure correct ID getGrpahId() is used?
    public Result execute(final String queryText, final Map<String, Object> queryParams) {
        return txn.execute(queryText, queryParams);
    }

    @Override
    public EvaluationContext createEvaluationContext(final GraphDatabaseService databaseService) {
        return new BasicEvaluationContext(txn, databaseService);
    }

    public Result execute(final String query) {
        return txn.execute(query);
    }

    @Override
    public List<GraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final GraphDirection direction,
                                                                EnumSet<TransportRelationshipTypes> relationshipTypes) {
        final GraphNode routeStationNode = findNode(routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        return routeStationNode.getRelationships(this, direction, relationshipTypes).toList();
    }

    public GraphNode wrapNode(final Node node) {
        return wrapNodeAsImmutable(node);
    }

    private GraphNode wrapNodeAsImmutable(final Node node) {
        final MutableGraphNodeNeo4J underlying = wrapNodeAsMutable(node);
        return new ImmutableGraphNode(underlying, sharedNodeCache);
    }

    private MutableGraphNodeNeo4J wrapNodeAsMutable(final Node node) {
        final GraphNodeId graphNodeId = idFactory.getIdFor(node);
        return new MutableGraphNodeNeo4J(node, graphNodeId, relationshipTypeFactory, sharedNodeCache.invalidatorFor(graphNodeId));
    }

    @Override
    public ImmutableGraphRelationshipNeo4J wrapRelationship(final Relationship relationship) {
        final GraphRelationshipId id = idFactory.getIdFor(relationship);
        final SharedRelationshipCache.InvalidatesCache invalidatesCache = sharedRelationshipCache.invalidatorFor(id);
        final MutableGraphRelationshipNeo4J underlying = new MutableGraphRelationshipNeo4J(relationship, id, relationshipTypeFactory, invalidatesCache);
        return new ImmutableGraphRelationshipNeo4J(underlying, sharedRelationshipCache);
    }

    MutableGraphRelationship wrapRelationshipMutable(final Relationship relationship) {
        final GraphRelationshipId id = idFactory.getIdFor(relationship);
        final SharedRelationshipCache.InvalidatesCache invalidatesCacheFor = sharedRelationshipCache.invalidatorFor(id);
        return new MutableGraphRelationshipNeo4J(relationship, id, relationshipTypeFactory, invalidatesCacheFor);
    }

    @Override
    public GraphNode fromStart(final Path path) {
        final Node startNode = path.startNode();
        if (startNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(startNode);
    }

    @Override
    public GraphNode fromEnd(final Path path) {
        final Node endNode = path.endNode();
        if (endNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(endNode);
    }

    @Override
    public GraphNodeId getStartNodeId(final Relationship relationship) {
        return idFactory.getIdFor(relationship.getStartNode());
    }

    @Override
    public GraphNodeId getEndNodeId(final Relationship relationship) {
        return idFactory.getIdFor(relationship.getEndNode());
    }

    @Override
    public GraphNodeId getGraphIdFor(final Node node) {
        return idFactory.getIdFor(node);
    }

//    @Override
//    public ImmutableGraphRelationship lastFrom(final Path path) {
//        final Relationship last = path.lastRelationship();
//        if (last==null) {
//            return null;
//        }
//        return wrapRelationship(last);
//    }

    @Override
    public Iterable<GraphNode> iter(final Iterable<Node> iterable) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<GraphNode> iterator() {
                return Streams.stream(iterable).map(node -> wrapNodeAsImmutable(node)).iterator();

            }
        };
    }

    @Override
    public GraphNode getStartNode(final Relationship relationship) {
        return wrapNodeAsImmutable(relationship.getStartNode());
    }

    @Override
    public GraphNode getEndNode(final Relationship relationship) {
        return wrapNodeAsImmutable(relationship.getEndNode());
    }

//    @Override
//    public GraphNodeId getPreviousNodeId(final GraphPath path) {
//        final GraphPathNeo4j graphPathNeo4j = (GraphPathNeo4j) path;
//        final Relationship last = graphPathNeo4j.lastRelationship();
//        if (last == null) {
//            return null;
//        } else {
//            return idFactory.getIdFor(last.getStartNode());
//        }
//    }

    /***
     * Diagnostic support only @See GraphTestHelper
     * Do not use
     * @param graphRelationship the relationship we want to find
     * @return the underlying object
     */
    Relationship unwrap(final GraphRelationship graphRelationship) {
        final String elementId = idFactory.getUnderlyingFor(graphRelationship.getId());
        return txn.getRelationshipByElementId(elementId);
    }

}
