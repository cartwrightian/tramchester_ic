package com.tramchester.graph.core.inMemory;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

class RelationshipsForNode {
    private final Set<RelationshipIdInMemory> outbound;
    private final Set<RelationshipIdInMemory> inbound;

    RelationshipsForNode() {
        outbound = new HashSet<>();
        inbound = new HashSet<>();
    }

    public Stream<GraphRelationshipInMemory> getOutbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> relationships) {
        return outbound.stream().map(relationships::get);
    }

    public Stream<GraphRelationshipInMemory> getInbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> relationships) {
        return inbound.stream().map(relationships::get);
    }

    public void addOutbound(final RelationshipIdInMemory relationshipId) {
        synchronized (outbound) {
            outbound.add(relationshipId);
        }
    }

    public void addInbound(final RelationshipIdInMemory relationshipId) {
        synchronized (inbound) {
            inbound.add(relationshipId);
        }
    }

    public void remove(final RelationshipIdInMemory relationshipId) {
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
