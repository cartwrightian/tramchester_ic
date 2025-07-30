package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.core.GraphRelationshipId;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Objects;

public class GraphRelationshipIdNeo4J implements GraphRelationshipId {
    private final String internalId;

    GraphRelationshipIdNeo4J(final String internalId) {
        this.internalId = internalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelationshipIdNeo4J that = (GraphRelationshipIdNeo4J) o;
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
