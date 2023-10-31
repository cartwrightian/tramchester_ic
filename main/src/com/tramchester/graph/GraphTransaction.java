package com.tramchester.graph;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/***
 * Facade around underlying graph DB Transaction
 */
public class GraphTransaction implements AutoCloseable {
    private final Transaction txn;

    GraphTransaction(Transaction txn) {
        this.txn = txn;
    }

    public void close() {
        txn.close();
    }

    public void commit() {
        txn.commit();
    }

    public GraphNode createNode(GraphLabel graphLabel) {
        return new GraphNode(txn.createNode(graphLabel));
    }

    public Schema schema() {
        return txn.schema();
    }

    @Deprecated
    public GraphNode getNodeById(Long nodeId) {
        return new GraphNode(txn.getNodeById(nodeId));
    }

    public GraphNode getNodeById(GraphNodeId nodeId) {
        return nodeId.findIn(txn);
    }

    public GraphNode createNode(Set<GraphLabel> labels) {
        GraphLabel[] toApply = new GraphLabel[labels.size()];
        labels.toArray(toApply);
        return new GraphNode(txn.createNode(toApply));
    }

    public Stream<GraphNode> findNodes(GraphLabel graphLabel) {
        return txn.findNodes(graphLabel).stream().map(GraphNode::new);
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
        return new GraphNode(node);
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
        return new GraphRelationship(txn.getRelationshipById(relationshipId));
    }

    public Result execute(String query) {
        return txn.execute(query);
    }

    public GraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId) {
        return graphRelationshipId.findIn(txn);
    }
}
