package com.tramchester.graph.search.inMemory;

import com.tramchester.graph.core.GraphNodeId;

import java.util.Objects;

public class SearchStateKey {
    private final GraphNodeId nodeId;

    public SearchStateKey(GraphNodeId nodeId) {
        this.nodeId = nodeId;
    }

    public GraphNodeId getNodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchStateKey stateKey = (SearchStateKey) o;
        return Objects.equals(nodeId, stateKey.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodeId);
    }

    @Override
    public String toString() {
        return "SearchStateKey{" +
                "nodeId=" + nodeId +
                '}';
    }
}
