package com.tramchester.graph.facade;

import java.util.Objects;

public class GraphRelationshipId {
    private final long legacyId;

    GraphRelationshipId(long legacyId) {
        this.legacyId = legacyId;
    }

    public static GraphRelationshipId TestOnly(long l) {
        return new GraphRelationshipId(l);
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

    public long getInternalId() {
        return legacyId;
    }

//    GraphRelationship findIn(Transaction txn) {
//        return new GraphRelationship(txn.getRelationshipById(legacyId), id);
//    }
}
