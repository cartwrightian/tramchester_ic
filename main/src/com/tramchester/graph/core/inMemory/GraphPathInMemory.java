package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.*;

public class GraphPathInMemory implements GraphPath {
    @Override
    public int length() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Iterable<GraphEntity> getEntities(GraphTransaction txn) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GraphNode getStartNode(GraphTransaction txn) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GraphNode getEndNode(GraphTransaction txn) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Iterable<GraphNode> getNodes(GraphTransaction txn) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GraphRelationship getLastRelationship(GraphTransaction txn) {
        throw new RuntimeException("Not implemented");
    }
}
