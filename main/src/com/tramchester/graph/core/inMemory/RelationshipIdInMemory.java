package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.*;
import com.tramchester.graph.core.GraphRelationshipId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RelationshipIdInMemory implements GraphRelationshipId, Comparable<RelationshipIdInMemory> {
    private final int id;

    @JsonCreator
    public RelationshipIdInMemory(
            @JsonProperty("id") int id) {
        this.id = id;
    }

    @JsonGetter("id")
    public int getIdForSave() {
        return id;
    }

    @Override
    public String toString() {
        return "RelationshipIdInMemory{" +
                "id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipIdInMemory that = (RelationshipIdInMemory) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public int compareTo(@NotNull RelationshipIdInMemory other) {
        return Integer.compare(this.id, other.id);
    }
}
