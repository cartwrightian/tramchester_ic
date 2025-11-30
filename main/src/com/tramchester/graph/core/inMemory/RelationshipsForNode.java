package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphRelationshipId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class RelationshipsForNode {
    private final Set<GraphRelationshipId> outbound;
    private final Set<GraphRelationshipId> inbound;

    RelationshipsForNode() {
        outbound = new HashSet<>();
        inbound = new HashSet<>();
    }

    public Stream<GraphRelationshipInMemory> getOutbound(final ConcurrentMap<GraphRelationshipId, GraphRelationshipInMemory> relationships) {
        return outbound.stream().map(relationships::get);
    }

    public Stream<GraphRelationshipInMemory> getInbound(final ConcurrentMap<GraphRelationshipId, GraphRelationshipInMemory> relationships) {
        return inbound.stream().map(relationships::get);
    }

    public void addOutbound(final GraphRelationshipId relationshipId) {
        synchronized (outbound) {
            outbound.add(relationshipId);
        }
    }

    public void addInbound(final GraphRelationshipId relationshipId) {
        synchronized (inbound) {
            inbound.add(relationshipId);
        }
    }

    public void remove(final GraphRelationshipId relationshipId) {
        synchronized (outbound) {
            outbound.remove(relationshipId);
        }
        synchronized (inbound) {
            inbound.remove(relationshipId);
        }
    }

    public boolean isEmpty() {
        return outbound.isEmpty() && inbound.isEmpty();
    }

    @Override
    public String toString() {
        return "RelationshipsForNode{" +
                "outbound=" + outbound +
                ", inbound=" + inbound +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipsForNode that = (RelationshipsForNode) o;
        return Objects.equals(outbound, that.outbound) && Objects.equals(inbound, that.inbound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outbound, inbound);
    }
}
