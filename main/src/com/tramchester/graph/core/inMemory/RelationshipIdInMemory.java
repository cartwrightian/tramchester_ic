package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.graph.core.GraphRelationshipId;
import org.jetbrains.annotations.NotNull;

public class RelationshipIdInMemory extends InternalGraphId implements GraphRelationshipId, Comparable<RelationshipIdInMemory>  {
    private final int id;
    private final int hash;

    @JsonCreator
    public RelationshipIdInMemory(@JsonProperty("id") int id) {
        this.id = id;
        this.hash = Integer.hashCode(id);
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
        return hash;
    }

    @Override
    public int compareTo(@NotNull RelationshipIdInMemory other) {
        return Integer.compare(this.id, other.id);
    }

    public void recordIdTo(final GraphIdFactory idFactory) {
        idFactory.captureRelationshipId(id);
    }

    @Override
    int getInternalId() {
        return id;
    }
}
