package com.tramchester.graph.facade;

import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;

public interface GraphPath {

    int length();

    Iterable<GraphEntity> getEntities(GraphTransaction txn);

    ImmutableGraphNode getStartNode(GraphTransaction txn);

    ImmutableGraphNode getEndNode(GraphTransaction txn);

    Iterable<ImmutableGraphNode> getNodes(GraphTransaction txn);

    GraphRelationship getLastRelationship(GraphTransaction txn);
}
