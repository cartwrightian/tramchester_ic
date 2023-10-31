package com.tramchester.graph.facade;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.apache.commons.lang3.stream.Streams;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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

    public GraphNode createNode(GraphLabel graphLabel) {
        Node node = txn.createNode(graphLabel);
        return wrapNode(node);
    }

    public Schema schema() {
        return txn.schema();
    }

    public GraphNode getNodeById(GraphNodeId nodeId) {
        long internalId = nodeId.getInternalId();
        Node node = txn.getNodeById(internalId);
        return wrapNode(node);
    }

    public GraphNode createNode(Set<GraphLabel> labels) {
        GraphLabel[] toApply = new GraphLabel[labels.size()];
        labels.toArray(toApply);
        Node node = txn.createNode(toApply);
        return wrapNode(node);
    }

    public Stream<GraphNode> findNodes(GraphLabel graphLabel) {
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

    private GraphNode findNode(GraphLabel label, GraphPropertyKey key, String value) {
        return findNode(label, key.getText(), value);
    }

    private GraphNode findNode(GraphLabel label, String key, String value) {
        Node node = txn.findNode(label, key, value);
        if (node==null) {
            return null;
        }
        return wrapNode(node);
    }

    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
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

    GraphNode wrapNode(Node endNode) {
        return new GraphNode(endNode, idFactory.getIdFor(endNode));
    }

    GraphRelationship wrapRelationship(Relationship relationship) {
        return new GraphRelationship(relationship, idFactory.getIdFor(relationship));
    }

    public GraphNode fromEnd(Path path) {
        Node endNode = path.endNode();
        if (endNode==null) {
            return null;
        }
        return wrapNode(endNode);
    }

    public GraphRelationship lastFrom(Path path) {
        Relationship last = path.lastRelationship();
        if (last==null) {
            return null;
        }
        return wrapRelationship(last);
    }

    public Iterable<GraphNode> iter(Iterable<Node> iterable) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<GraphNode> iterator() {
                return Streams.of(iterable).map(node -> wrapNode(node)).iterator();

            }
        };
    }

}
