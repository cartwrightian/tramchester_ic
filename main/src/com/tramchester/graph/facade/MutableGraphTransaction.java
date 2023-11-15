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
        return wrapRelationship(relationship);
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

    private MutableGraphNode findNodeMutable(GraphLabel label, GraphPropertyKey key, String value) {
        return findNodeMutable(label, key.getText(), value);
    }

    private ImmutableGraphNode findNode(GraphLabel label, GraphPropertyKey key, String value) {
        return findNode(label, key.getText(), value);
    }

    private ImmutableGraphNode findNode(GraphLabel label, String key, String value) {
        Node node = txn.findNode(label, key, value);
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
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmutableGraphNode findNode(ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(ITEM item) {
        return findNodeMutable(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    public Result execute(String queryText, Map<String, Object> queryParams) {
        return txn.execute(queryText, queryParams);
    }

    @Override
    public EvaluationContext createEvaluationContext(GraphDatabaseService databaseService) {
        return new BasicEvaluationContext(txn, databaseService);
    }

    public Result execute(String query) {
        return txn.execute(query);
    }

    @Override
    public List<ImmutableGraphRelationship> getRouteStationRelationships(RouteStation routeStation, Direction direction) {
        GraphNode routeStationNode = findNode(routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        return routeStationNode.getRelationships(this, direction, TransportRelationshipTypes.forPlanning()).toList();
    }

    public MutableGraphNode wrapNode(Node endNode) {
        GraphNodeId graphNodeId = idFactory.getIdFor(endNode);
        return new MutableGraphNode(endNode, graphNodeId);
    }

    public MutableGraphNode wrapNodeAsMutable(Node endNode) {
        GraphNodeId graphNodeId = idFactory.getIdFor(endNode);
        return new MutableGraphNode(endNode, graphNodeId);
    }

    ImmutableGraphNode wrapNodeAsImmutable(Node endNode) {
        MutableGraphNode underlying = wrapNodeAsMutable(endNode);
        return new ImmutableGraphNode(underlying);
    }

    @Override
    public ImmutableGraphRelationship wrapRelationship(Relationship relationship) {
        MutableGraphRelationship underlying = new MutableGraphRelationship(relationship, idFactory.getIdFor(relationship));
        return new ImmutableGraphRelationship(underlying);
    }

    public MutableGraphRelationship wrapRelationshipMutable(Relationship relationship) {
        return new MutableGraphRelationship(relationship, idFactory.getIdFor(relationship));
    }

    public ImmutableGraphNode fromStart(Path path) {
        final Node startNode = path.startNode();
        if (startNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(startNode);
    }

    public ImmutableGraphNode fromEnd(Path path) {
        final Node endNode = path.endNode();
        if (endNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(endNode);
    }

    public ImmutableGraphRelationship lastFrom(Path path) {
        Relationship last = path.lastRelationship();
        if (last==null) {
            return null;
        }
        return wrapRelationship(last);
    }

    @Override
    public Iterable<ImmutableGraphNode> iter(Iterable<Node> iterable) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<ImmutableGraphNode> iterator() {
                return Streams.stream(iterable).map(node -> wrapNodeAsImmutable(node)).iterator();

            }
        };
    }

    public GraphRelationship getQueryColumnAsRelationship(Map<String, Object> row, String columnName) {
        Relationship relationship = (Relationship) row.get(columnName);
        return wrapRelationship(relationship);
    }

    public GraphNodeId createNodeId(Node endNode) {
        return idFactory.getNodeIdFor(endNode.getElementId());
    }

    @Override
    public ImmutableGraphNode getStartNode(Relationship relationship) {
        return wrapNodeAsImmutable(relationship.getStartNode());
    }
}
