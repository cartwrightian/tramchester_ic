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

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.COST;

/***
 * Facade around underlying graph DB Transaction
 */
public class GraphTransaction implements AutoCloseable {
    private final Transaction txn;
    private final GraphIdFactory idFactory;

    GraphTransaction(Transaction txn, GraphIdFactory idFactory) {
        this.txn = txn;
        this.idFactory = idFactory;
    }

    public void close() {
        txn.close();
    }

    public void commit() {
        txn.commit();
    }

    public MutableGraphNode createNode(GraphLabel graphLabel) {
        Node node = txn.createNode(graphLabel);
        return wrapNode(node);
    }

    public Schema schema() {
        return txn.schema();
    }

    public GraphNode getNodeById(GraphNodeId nodeId) {
        long internalId = nodeId.getInternalId();
        Node node = txn.getNodeById(internalId);
        return wrapNodeAsImmutable(node);
    }

    public MutableGraphNode getNodeByIdMutable(GraphNodeId nodeId) {
        long internalId = nodeId.getInternalId();
        Node node = txn.getNodeById(internalId);
        return wrapNode(node);
    }

    public MutableGraphNode createNode(Set<GraphLabel> labels) {
        GraphLabel[] toApply = new GraphLabel[labels.size()];
        labels.toArray(toApply);
        Node node = txn.createNode(toApply);
        return wrapNode(node);
    }

    public Stream<GraphNode> findNodes(GraphLabel graphLabel) {
        return txn.findNodes(graphLabel).stream().map(this::wrapNodeAsImmutable);
    }

    public Stream<MutableGraphNode> findNodesMutable(GraphLabel graphLabel) {
        return txn.findNodes(graphLabel).stream().map(this::wrapNode);
    }

    public boolean hasAnyMatching(GraphLabel label, String field, String value) {
        Node node = txn.findNode(label, field, value);
        return node != null;
    }

    public boolean hasAnyMatching(GraphLabel graphLabel) {
        ResourceIterator<Node> found = txn.findNodes(graphLabel);
        List<Node> nodes = found.stream().toList();
        return !nodes.isEmpty();
    }

    private MutableGraphNode findNodeMutable(GraphLabel label, GraphPropertyKey key, String value) {
        return findNodeMutable(label, key.getText(), value);
    }

    private GraphNode findNode(GraphLabel label, GraphPropertyKey key, String value) {
        return findNode(label, key.getText(), value);
    }

    private GraphNode findNode(GraphLabel label, String key, String value) {
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
        return wrapNode(node);
    }

    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(ITEM item) {
        return findNodeMutable(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    public Result execute(String queryText, Map<String, Object> queryParams) {
        return txn.execute(queryText, queryParams);
    }

    public EvaluationContext createEvaluationContext(GraphDatabaseService databaseService) {
        return new BasicEvaluationContext(txn, databaseService);
    }

    @Deprecated
    public GraphRelationship getRelationshipById(long relationshipId) {
        Relationship relationship = txn.getRelationshipById(relationshipId);
        return wrapRelationship(relationship);
    }

    public Result execute(String query) {
        return txn.execute(query);
    }

    public GraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId) {
        Relationship relationship = txn.getRelationshipById(graphRelationshipId.getInternalId());
        if (relationship==null) {
            return null;
        }
        return wrapRelationship(relationship);
    }

    public List<GraphRelationship> getRouteStationRelationships(RouteStation routeStation, Direction direction) {
        GraphNode routeStationNode = findNode(routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        return routeStationNode.getRelationships(this, direction, TransportRelationshipTypes.forPlanning()).toList();
    }

    MutableGraphNode wrapNode(Node endNode) {
        GraphNodeId graphNodeId = idFactory.getIdFor(endNode);
        return new MutableGraphNode(endNode, graphNodeId);
    }

    ImmuableGraphNode wrapNodeAsImmutable(Node endNode) {
        MutableGraphNode underlying = wrapNode(endNode);
        return new ImmuableGraphNode(underlying);
    }

    GraphRelationship wrapRelationship(Relationship relationship) {
        return new GraphRelationship(relationship, idFactory.getIdFor(relationship));
    }

    public GraphNode fromStart(Path path) {
        final Node startNode = path.startNode();
        if (startNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(startNode);
    }

    public GraphNode fromEnd(Path path) {
        final Node endNode = path.endNode();
        if (endNode==null) {
            return null;
        }
        return wrapNodeAsImmutable(endNode);
    }

    public GraphRelationship lastFrom(Path path) {
        Relationship last = path.lastRelationship();
        if (last==null) {
            return null;
        }
        return wrapRelationship(last);
    }

    public Iterable<ImmuableGraphNode> iter(Iterable<Node> iterable) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<ImmuableGraphNode> iterator() {
                return Streams.stream(iterable).map(node -> wrapNodeAsImmutable(node)).iterator();

            }
        };
    }

    public GraphNodeId createNodeId(long legacyId) {
        return idFactory.getNodeIdFor(legacyId);
    }

    public GraphRelationship getQueryColumnAsRelationship(Map<String, Object> row, String columnName) {
        Relationship relationship = (Relationship) row.get(columnName);
        return wrapRelationship(relationship);
    }

    public Duration totalDurationFor(Path path) {
        return Streams.stream(path.relationships()).
                filter(relationship -> relationship.hasProperty(GraphPropertyKey.COST.getText())).
                map(GraphTransaction::getCost).
                reduce(Duration.ZERO, Duration::plus);
    }

    private static Duration getCost(Relationship relationship) {
        final int value = (int) relationship.getProperty(COST.getText());
        return Duration.ofMinutes(value);
    }
}