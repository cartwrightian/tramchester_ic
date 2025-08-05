package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphRelationshipId;

import java.util.Objects;

public class RelationshipIdInMemory implements GraphRelationshipId {
    private final int id;

    public RelationshipIdInMemory(int id) {
        this.id = id;
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
}
