package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.reference.GraphLabel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class NodeIdInMemory implements GraphNodeId, Comparable<NodeIdInMemory> {
    private final int id;

    @JsonIgnore
    private final ImmutableEnumSet<GraphLabel> labels; // diagnostics only

    @JsonCreator
    public NodeIdInMemory(@JsonProperty("id") final int id) {
        this(id, GraphLabel.NoneOf);
    }

    public NodeIdInMemory(final int id, final ImmutableEnumSet<GraphLabel> labels) {
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

    void recordIdTo(final GraphIdFactory toUpdate) {
        toUpdate.captureNodeId(id);
    }
}
