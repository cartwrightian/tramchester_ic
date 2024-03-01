package com.tramchester.graph.facade;

import com.google.common.collect.Streams;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;

import java.util.*;
import java.util.stream.Stream;

/***
 * Facade around underlying graph DB Transaction
 */
public class MutableGraphTransaction implements GraphTransaction {
    private final Transaction txn;
    private final GraphIdFactory idFactory;

    MutableGraphTransaction(Transaction txn, GraphIdFactory idFactory) {
        this.txn = txn;
        this.idFactory = idFactory;
    }

    @Override
    public void close() {
        txn.close();
    }

    public void commit() {
        txn.commit();
    }

    public MutableGraphNode createNode(GraphLabel graphLabel) {
        Node node = txn.createNode(graphLabel);
        return wrapNodeAsMutable(node);
    }

    public Schema schema() {
        return txn.schema();
    }

    @Override
    public ImmutableGraphNode getNodeById(GraphNodeId nodeId) {
        Node node = nodeId.getNodeFrom(txn);
        return wrapNodeAsImmutable(node);
    }

    public MutableGraphNode getNodeByIdMutable(GraphNodeId nodeId) {
        Node node = nodeId.getNodeFrom(txn);
        return wrapNodeAsMutable(node);
    }

    public ImmutableGraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId) {
        Relationship relationship = graphRelationshipId.getRelationshipFrom(txn);
        if (relationship==null) {
            return null;
        }
        return wrapRelationship(relationship, TransportRelationshipTypes.from(relationship));
    }

    public MutableGraphNode createNode(Set<GraphLabel> labels) {
        GraphLabel[] toApply = new GraphLabel[labels.size()];
        labels.toArray(toApply);
        Node node = txn.createNode(toApply);
        return wrapNodeAsMutable(node);
    }

    @Override
    public Stream<ImmutableGraphNode> findNodes(GraphLabel graphLabel) {
        return txn.findNodes(graphLabel).stream().map(this::wrapNodeAsImmutable);
    }

    public Stream<MutableGraphNode> findNodesMutable(GraphLabel graphLabel) {
        return txn.findNodes(graphLabel).stream().map(this::wrapNodeAsMutable);
    }

    @Override
    public boolean hasAnyMatching(GraphLabel label, String field, String value) {
        Node node = txn.findNode(label, field, value);
        return node != null;
    }

    @Override
    public boolean hasAnyMatching(GraphLabel graphLabel) {
        ResourceIterator<Node> found = txn.findNodes(graphLabel);
        List<Node> nodes = found.stream().toList();
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

    private MutableGraphNode findNodeMutable(GraphLabel label, String key, String value) {
        Node node = txn.findNode(label, key, value);
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
    public List<ImmutableGraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final Direction direction) {
        final GraphNode routeStationNode = findNode(routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        return routeStationNode.getRelationships(this, direction, TransportRelationshipTypes.forPlanning()).toList();
    }

    public MutableGraphNode wrapNode(final Node endNode) {
        final GraphNodeId graphNodeId = idFactory.getIdFor(endNode);
        return new MutableGraphNode(endNode, graphNodeId);
    }

    public MutableGraphNode wrapNodeAsMutable(final Node endNode) {
        final GraphNodeId graphNodeId = idFactory.getIdFor(endNode);
        return new MutableGraphNode(endNode, graphNodeId);
    }

    ImmutableGraphNode wrapNodeAsImmutable(final Node endNode) {
        final MutableGraphNode underlying = wrapNodeAsMutable(endNode);
        return new ImmutableGraphNode(underlying);
    }

    @Override
    public ImmutableGraphRelationship wrapRelationship(final Relationship relationship, TransportRelationshipTypes relationshipType) {
        final MutableGraphRelationship underlying = new MutableGraphRelationship(relationship, idFactory.getIdFor(relationship), relationshipType);
        return new ImmutableGraphRelationship(underlying);
    }

    public MutableGraphRelationship wrapRelationshipMutable(final Relationship relationship, TransportRelationshipTypes relationshipType) {
        return new MutableGraphRelationship(relationship, idFactory.getIdFor(relationship), relationshipType);
    }

    public ImmutableGraphNode fromStart(final Path path) {
        final Node startNode = path.startNode();
        if (startNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(startNode);
    }

    public ImmutableGraphNode fromEnd(final Path path) {
        final Node endNode = path.endNode();
        if (endNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(endNode);
    }

    public ImmutableGraphRelationship lastFrom(final Path path) {
        final Relationship last = path.lastRelationship();
        if (last==null) {
            return null;
        }
        return wrapRelationship(last, TransportRelationshipTypes.from(last));
    }

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

    public GraphRelationship getQueryColumnAsRelationship(final Map<String, Object> row, final String columnName) {
        final Relationship relationship = (Relationship) row.get(columnName);
        return wrapRelationship(relationship, TransportRelationshipTypes.from(relationship));
    }

    public GraphNodeId createNodeId(final Node endNode) {
        return idFactory.getNodeIdFor(endNode.getElementId());
    }

    @Override
    public ImmutableGraphNode getStartNode(final Relationship relationship) {
        return wrapNodeAsImmutable(relationship.getStartNode());
    }

    @Override
    public ImmutableGraphNode getEndNode(final Relationship relationship) {
        return wrapNodeAsImmutable(relationship.getEndNode());
    }
}
