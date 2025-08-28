package com.tramchester.graph.core;

import com.tramchester.graph.core.inMemory.GraphPathInMemory;

import java.time.Duration;

public interface GraphPath {

    int length();

    Duration getTotalCost();

    Iterable<GraphEntity> getEntities(GraphTransaction txn);

    GraphNode getStartNode(GraphTransaction txn);

    GraphNode getEndNode(GraphTransaction txn);

    Iterable<GraphNode> getNodes(GraphTransaction txn);

    GraphRelationship getLastRelationship(GraphTransaction txn);

    GraphNodeId getPreviousNodeId(GraphTransaction txn);

    GraphPathInMemory duplicateWith(GraphTransaction txn, GraphNode node);

}
