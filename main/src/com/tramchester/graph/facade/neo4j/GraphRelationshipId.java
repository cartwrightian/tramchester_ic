package com.tramchester.graph.facade.neo4j;

import com.tramchester.graph.facade.GraphId;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Objects;

public class GraphRelationshipId implements GraphId {
    private final String internalId;

    GraphRelationshipId(final String internalId) {
        this.internalId = internalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelationshipId that = (GraphRelationshipId) o;
        return internalId.equals(that.internalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalId);
    }

    @Override
    public String toString() {
        return "GraphRelationshipId{" +
                "internalId=" + internalId +
                '}';
    }

    Relationship getRelationshipFrom(final Transaction txn) {
        return txn.getRelationshipByElementId(internalId);
    }
}
