package com.tramchester.graph.core;

import com.tramchester.domain.time.TramDuration;

import java.util.stream.Stream;

public interface GraphPath {

    int length();

    TramDuration getTotalCost();

    Iterable<GraphEntity<? extends GraphId>> getEntities(GraphTransaction txn);

    Stream<GraphEntity<? extends GraphId>> getEntityStream(GraphTransaction txn);

    GraphNode getStartNode(GraphTransaction txn);

    GraphNode getEndNode(GraphTransaction txn);

    Iterable<GraphNode> getNodes(GraphTransaction txn);

    GraphRelationship getLastRelationship(GraphTransaction txn);

    GraphNodeId getPreviousNodeId(GraphTransaction txn);

    GraphPath duplicateWith(GraphTransaction txn, GraphNode node);

    GraphPath duplicateWith(GraphTransaction txn, GraphRelationship graphRelationship);

    String displayPath();
}
