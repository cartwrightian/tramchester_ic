package com.tramchester.graph.facade.neo4j;

import com.google.common.collect.Streams;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.caches.SharedRelationshipCache;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Stream;

/***
 * Facade around underlying graph DB Transaction
 */
public class MutableGraphTransactionNeo4J implements GraphTransaction, GraphTransactionNeo4J, AutoCloseable {
    private final Transaction txn;
    private final GraphIdFactory idFactory;
    private final TransactionObserver transactionObserver;
    private final int transactionId;

    /*
     these cache attributes of the nodes, not the nodes themselves as these cannot be accessed outside of the
     parent transaction
    */
    private final SharedNodeCache sharedNodeCache;
    private final SharedRelationshipCache sharedRelationshipCache;

    /* GraphIdFactory ownership is passed in */
    protected MutableGraphTransactionNeo4J(final Transaction txn, final GraphIdFactory idFactory, final int transactionId,
                                           final TransactionObserver transactionObserver, SharedNodeCache sharedNodeCache,
                                           SharedRelationshipCache sharedRelationshipCache) {
        this.txn = txn;
        this.idFactory = idFactory;
        this.transactionId = transactionId;
        this.transactionObserver = transactionObserver;
        this.sharedNodeCache = sharedNodeCache;
        this.sharedRelationshipCache = sharedRelationshipCache;
    }

    /***
     * Use with care, this original transaction must not be used any further but must be closed
     * @return GraphTransaction
     */
    public ImmutableGraphTransactionNeo4J asImmutable() {
        return new ImmutableGraphTransactionNeo4J(this);
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

    public MutableGraphNode createNode(final GraphLabel graphLabel) {
        final Node node = txn.createNode(graphLabel);
        return wrapNodeAsMutable(node);
    }

    public MutableGraphNode createNode(final EnumSet<GraphLabel> labels) {
        final GraphLabel[] toApply = new GraphLabel[labels.size()];
        labels.toArray(toApply);
        final Node node = txn.createNode(toApply);
        return wrapNodeAsMutable(node);
    }

    public DBSchema schema() {
        return new DBSchema(txn.schema());
    }

    @Override
    public ImmutableGraphNode getNodeById(final GraphNodeId nodeId) {
        final Node node = nodeId.getNodeFrom(txn);
        return wrapNodeAsImmutable(node);
    }

    public MutableGraphNode getNodeByIdMutable(final GraphNodeId nodeId) {
        final Node node = nodeId.getNodeFrom(txn);
        return wrapNodeAsMutable(node);
    }

    @Override
    public ImmutableGraphRelationship getRelationshipById(final GraphRelationshipId graphRelationshipId) {
        final Relationship relationship = graphRelationshipId.getRelationshipFrom(txn);
        if (relationship==null) {
            return null;
        }
        return wrapRelationship(relationship);
    }

    @Override
    public Stream<ImmutableGraphNode> findNodes(final GraphLabel graphLabel) {
        return txn.findNodes(graphLabel).stream().map(this::wrapNodeAsImmutable);
    }

    public Stream<MutableGraphNode> findNodesMutable(GraphLabel graphLabel) {
        return txn.findNodes(graphLabel).stream().map(this::wrapNodeAsMutable);
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel label, final String field, final String value) {
        Node node = txn.findNode(label, field, value);
        return node != null;
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel graphLabel) {
        final ResourceIterator<Node> found = txn.findNodes(graphLabel);
        final List<Node> nodes = found.stream().toList();
        return !nodes.isEmpty();
    }

    private MutableGraphNode findNodeMutable(final GraphLabel label, final GraphPropertyKey key, final String value) {
        return findNodeMutable(label, key.getText(), value);
    }

    private ImmutableGraphNode findNode(final GraphLabel label, final GraphPropertyKey key, final String value) {
        return findNode(label, key.getText(), value);
    }

    private ImmutableGraphNode findNode(final GraphLabel label, final String key, final String value) {
        final Node node = txn.findNode(label, key, value);
        if (node==null) {
            return null;
        }
        return wrapNodeAsImmutable(node);
    }

    private MutableGraphNode findNodeMutable(final GraphLabel label, final String key, final String value) {
        final Node node = txn.findNode(label, key, value);
        if (node==null) {
            return null;
        }
        return wrapNodeAsMutable(node);
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmutableGraphNode findNode(final ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

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
    public List<ImmutableGraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final GraphDirection direction) {
        final GraphNode routeStationNode = findNode(routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        return routeStationNode.getRelationships(this, direction, TransportRelationshipTypes.forPlanning()).toList();
    }

    public GraphNode wrapNode(final Node node) {
        return wrapNodeAsImmutable(node);
    }

    private ImmutableGraphNode wrapNodeAsImmutable(final Node node) {
        final MutableGraphNode underlying = wrapNodeAsMutable(node);
        return new ImmutableGraphNode(underlying, sharedNodeCache);
    }

    private MutableGraphNode wrapNodeAsMutable(final Node node) {
        final GraphNodeId graphNodeId = idFactory.getIdFor(node);
        return new MutableGraphNode(node, graphNodeId, sharedNodeCache.invalidatorFor(graphNodeId));
    }

    @Override
    public ImmutableGraphRelationship wrapRelationship(final Relationship relationship) {
        final GraphRelationshipId id = idFactory.getIdFor(relationship);
        final SharedRelationshipCache.InvalidatesCache invalidatesCache = sharedRelationshipCache.invalidatorFor(id);
        final MutableGraphRelationship underlying = new MutableGraphRelationship(relationship, id, invalidatesCache);
        return new ImmutableGraphRelationship(underlying, sharedRelationshipCache);
    }

    MutableGraphRelationship wrapRelationshipMutable(final Relationship relationship) {
        final GraphRelationshipId id = idFactory.getIdFor(relationship);
        final SharedRelationshipCache.InvalidatesCache invalidatesCacheFor = sharedRelationshipCache.invalidatorFor(id);
        return new MutableGraphRelationship(relationship, id, invalidatesCacheFor);
    }

    @Override
    public ImmutableGraphNode fromStart(final Path path) {
        final Node startNode = path.startNode();
        if (startNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(startNode);
    }

    @Override
    public ImmutableGraphNode fromEnd(final Path path) {
        final Node endNode = path.endNode();
        if (endNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(endNode);
    }

    @Override
    public GraphNodeId endNodeNodeId(final Path path) {
        final Node endNode = path.endNode();
        return idFactory.getIdFor(endNode);
    }

    @Override
    public GraphNodeId getStartNodeId(final Relationship relationship) {
        return idFactory.getIdFor(relationship.getStartNode());
    }

    @Override
    public GraphNodeId getEndNodeId(final Relationship relationship) {
        return idFactory.getIdFor(relationship.getEndNode());
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
    public Iterable<ImmutableGraphNode> iter(final Iterable<Node> iterable) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<ImmutableGraphNode> iterator() {
                return Streams.stream(iterable).map(node -> wrapNodeAsImmutable(node)).iterator();

            }
        };
    }

    public ImmutableGraphRelationship getQueryColumnAsRelationship(final Map<String, Object> row, final String columnName) {
        final Relationship relationship = (Relationship) row.get(columnName);
        return wrapRelationship(relationship);
    }

    @Override
    public ImmutableGraphNode getStartNode(final Relationship relationship) {
        return wrapNodeAsImmutable(relationship.getStartNode());
    }

    @Override
    public ImmutableGraphNode getEndNode(final Relationship relationship) {
        return wrapNodeAsImmutable(relationship.getEndNode());
    }

    @Override
    public GraphNodeId getPreviousNodeId(final Path path) {
        final Relationship last = path.lastRelationship();
        if (last == null) {
            return null;
        } else {
            return idFactory.getIdFor(last.getStartNode());
        }
//        final GraphRelationship graphRelationship = result;
//
//        if (graphRelationship==null) {
//            return null;
//        }
//        return graphRelationship.getStartNodeId(txn);
    }


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
