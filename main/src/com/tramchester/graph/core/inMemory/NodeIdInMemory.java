package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.*;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.reference.GraphLabel;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


//@JsonTypeName(value = "nodeId")
//@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class NodeIdInMemory implements GraphNodeId, Comparable<NodeIdInMemory> {
    private final int id;
    @JsonIgnore
    private final EnumSet<GraphLabel> labels; // diagnostics only

    @JsonCreator
    public NodeIdInMemory(@JsonProperty("id") final int id) {
        this(id, EnumSet.noneOf(GraphLabel.class));
    }

    public NodeIdInMemory(final int id, final EnumSet<GraphLabel> labels) {
        this.id = id;
        this.labels = labels;
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
        if (labels.isEmpty()) {
            return "NodeId{" +
                    "id=" + id +
                    '}';
        } else {
            return "NodeId{" +
                    "id=" + id +
                    " " +labels +
                    '}';
        }

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public int compareTo(@NotNull NodeIdInMemory other) {
        return Integer.compare(this.id, other.id);
    }

    void recordIdTo(final AtomicInteger toUpdate) {
        toUpdate.set(id);
    }
}
