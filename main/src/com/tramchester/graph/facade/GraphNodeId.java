package com.tramchester.graph.facade;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Objects;

public class GraphNodeId {
    private final String internalId;
    private final int hashCode;

    GraphNodeId(String internalId) {
        // todo performance, intern this id?
        this.internalId = internalId;
        this.hashCode = Objects.hash(internalId);
    }

    public static GraphNodeId TestOnly(long l) {
        return new GraphNodeId(Long.toString(l));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNodeId that = (GraphNodeId) o;
        return internalId.equals(that.internalId);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "GraphNodeId{" +
                "legacyId=" + internalId +
                '}';
    }

    Node getNodeFrom(Transaction txn) {
        return txn.getNodeByElementId(internalId);
        //return txn.getNodeById(internalId);
    }
}
