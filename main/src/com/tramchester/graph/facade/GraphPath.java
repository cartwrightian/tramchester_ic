package com.tramchester.graph.facade;

import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;

public interface GraphPath {

    int length();

    Iterable<GraphEntity> getEntities(GraphTransactionNeo4J txn);

    ImmutableGraphNode getStartNode(GraphTransactionNeo4J txn);

    ImmutableGraphNode getEndNode(GraphTransactionNeo4J txn);

    Iterable<ImmutableGraphNode> getNodes(GraphTransactionNeo4J txn);

    GraphRelationship getLastRelationship(GraphTransactionNeo4J txn);
}
