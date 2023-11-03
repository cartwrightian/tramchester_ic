package com.tramchester.graph.facade;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Objects;

public class GraphRelationshipId {
    private final String internalId;

    GraphRelationshipId(String internalId) {
        this.internalId = internalId;
    }

    public static GraphRelationshipId TestOnly(long l) {
        return new GraphRelationshipId(Long.toString(l));
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
                "legacyId=" + internalId +
                '}';
    }

    Relationship getRelationshipFrom(Transaction txn) {
        return txn.getRelationshipByElementId(internalId); //txn.getRelationshipById(internalId);
    }
}
