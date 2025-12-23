package com.tramchester.graph.search.inMemory;

import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;

import java.util.List;
import java.util.Objects;

public class SearchStateKey {
    private final GraphNodeId nodeId;
    private final List<GraphId> path;

    public static SearchStateKey create(GraphPathInMemory graphPath, GraphNodeId lastNode) {
        return new SearchStateKey(lastNode, graphPath.getEntitiesIds());
    }

    public SearchStateKey(GraphNodeId nodeId, List<GraphId> path) {
        this.nodeId = nodeId;
        this.path = path;
    }

    public GraphNodeId getNodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchStateKey stateKey = (SearchStateKey) o;
        return Objects.equals(nodeId, stateKey.nodeId) && Objects.equals(path, stateKey.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, path);
    }

    @Override
    public String toString() {
        return "SearchStateKey{" +
                "nodeId=" + nodeId +
                ", path=" + path +
                '}';
    }
}
