package com.tramchester.graph;

import java.util.Objects;

public class GraphNodeId {
    private final long legacyId;

    public GraphNodeId(long legacyId) {
        this.legacyId = legacyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNodeId that = (GraphNodeId) o;
        return legacyId == that.legacyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(legacyId);
    }

    @Override
    public String toString() {
        return "GraphNodeId{" +
                "legacyId=" + legacyId +
                '}';
    }
}
