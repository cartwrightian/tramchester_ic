package com.tramchester.graph.facade;

import java.util.Objects;

public class GraphNodeId {
    private final long legacyId;

    GraphNodeId(long legacyId) {
        this.legacyId = legacyId;
    }

    public static GraphNodeId TestOnly(long l) {
        return new GraphNodeId(l);
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

    long getInternalId() {
        return legacyId;
    }

}
