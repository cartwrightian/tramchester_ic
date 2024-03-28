package com.tramchester.graph.facade;

import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.EnumSet;
import java.util.Objects;

public class GraphNodeId {
    private final String internalId;
    private final EnumSet<GraphLabel> labels;
    private final int hashCode;

    // NOTE: labels not provided unless DB diagnostics set to true

    GraphNodeId(String internalId, EnumSet<GraphLabel> labels) {
        // todo performance, intern this id?
        this.internalId = internalId;
        this.labels = labels;
        this.hashCode = Objects.hash(internalId);
    }

    public static GraphNodeId TestOnly(long l) {
        return new GraphNodeId(Long.toString(l), EnumSet.noneOf(GraphLabel.class));
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
                "internalId='" + internalId + '\'' +
                ", labels=" + labels +
                '}';
    }

    Node getNodeFrom(final Transaction txn) {
        return txn.getNodeByElementId(internalId);
    }
}
