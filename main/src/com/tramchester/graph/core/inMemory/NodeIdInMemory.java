package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphNodeId;

import java.util.Objects;

public class NodeIdInMemory implements GraphNodeId {
    private final int id;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NodeIdInMemory that = (NodeIdInMemory) o;
        return id == that.id;
    }

    @Override
    public String toString() {
        return "NodeIdInMemory{" +
                "id=" + id +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public NodeIdInMemory(int id) {
        this.id = id;
    }
}
