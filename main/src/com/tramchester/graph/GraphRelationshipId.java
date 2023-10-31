package com.tramchester.graph;

import org.neo4j.graphdb.Transaction;

import java.util.Objects;

public class GraphRelationshipId {
    private final long legacyId;

    public GraphRelationshipId(long legacyId) {
        this.legacyId = legacyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelationshipId that = (GraphRelationshipId) o;
        return legacyId == that.legacyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(legacyId);
    }

    @Override
    public String toString() {
        return "GraphRelationshipId{" +
                "legacyId=" + legacyId +
                '}';
    }

    public GraphRelationship findIn(Transaction txn) {
        return new GraphRelationship(txn.getRelationshipById(legacyId));
    }
}
