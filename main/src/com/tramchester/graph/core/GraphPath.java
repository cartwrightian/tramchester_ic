package com.tramchester.graph.core;

public interface GraphPath {

    int length();

    Iterable<GraphEntity> getEntities(GraphTransaction txn);

    GraphNode getStartNode(GraphTransaction txn);

    GraphNode getEndNode(GraphTransaction txn);

    Iterable<GraphNode> getNodes(GraphTransaction txn);

    GraphRelationship getLastRelationship(GraphTransaction txn);
}
