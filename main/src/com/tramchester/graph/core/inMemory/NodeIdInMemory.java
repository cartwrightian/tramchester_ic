package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.*;
import com.tramchester.graph.core.GraphNodeId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


@JsonTypeName(value = "nodeId")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class NodeIdInMemory implements GraphNodeId, Comparable<NodeIdInMemory> {
    private final int id;

    @JsonCreator
    public NodeIdInMemory(@JsonProperty("id") final int id) {
        this.id = id;
    }

    @JsonGetter("id")
    public int getIdForSave() {
        return id;
    }

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

    @Override
    public int compareTo(@NotNull NodeIdInMemory other) {
        return Integer.compare(this.id, other.id);
    }
}
