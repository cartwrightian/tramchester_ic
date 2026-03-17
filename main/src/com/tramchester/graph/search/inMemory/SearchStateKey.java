package com.tramchester.graph.search.inMemory;

import com.tramchester.graph.core.GraphIdList;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;

import java.util.Objects;

public class SearchStateKey {
    private final GraphNodeId nodeId;
    private final GraphIdList path;

    public static SearchStateKey create(final GraphPathInMemory graphPath, final GraphNodeId lastNode) {
        return new SearchStateKey(lastNode, graphPath.getEntitiesIds());
    }

    public SearchStateKey(final GraphNodeId nodeId, final GraphIdList path) {
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
