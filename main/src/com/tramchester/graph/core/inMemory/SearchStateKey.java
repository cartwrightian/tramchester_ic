package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphIdList;
import com.tramchester.graph.core.GraphNodeId;

import java.util.Objects;

public class SearchStateKey {
    private final GraphNodeId nodeId;
    private final GraphIdList path;
    private final int internalNodeId;

    public static SearchStateKey create(final GraphPathInMemory graphPath, final GraphNodeId lastNode) {
        return new SearchStateKey(lastNode, graphPath.getEntitiesIds());
    }

    public SearchStateKey(final GraphNodeId nodeId, final GraphIdList path) {
        this.nodeId = nodeId;
        this.path = path;
        internalNodeId = ((InternalGraphId)nodeId).getInternalId();
    }

    public GraphNodeId getNodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        SearchStateKey that = (SearchStateKey) object;
        return internalNodeId == that.internalNodeId && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, internalNodeId);
    }

    @Override
    public String toString() {
        return "SearchStateKey{" +
                "nodeId=" + nodeId +
                ", path=" + path +
                '}';
    }
}
