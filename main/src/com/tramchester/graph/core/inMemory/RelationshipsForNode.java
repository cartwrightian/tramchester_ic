package com.tramchester.graph.core.inMemory;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

class RelationshipsForNode {
    private final Set<RelationshipIdInMemory> outboundIds;
    private final Set<RelationshipIdInMemory> inboundIds;

    private static final RelationshipsForNode empty = new RelationshipsForNode();

    RelationshipsForNode() {
        outboundIds = new HashSet<>();
        inboundIds = new HashSet<>();
    }

    public static RelationshipsForNode empty() {
        return empty;
    }

    public Stream<GraphRelationshipInMemory> getOutbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source) {
        return outboundIds.stream().map(source::get);
    }

    public Stream<GraphRelationshipInMemory> getInbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source) {
        return inboundIds.stream().map(source::get);
    }

    public void addOutbound(final RelationshipIdInMemory relationshipId) {
        synchronized (outboundIds) {
            outboundIds.add(relationshipId);
        }
    }

    public void addInbound(final RelationshipIdInMemory relationshipId) {
        synchronized (inboundIds) {
            inboundIds.add(relationshipId);
        }
    }

    public void remove(final RelationshipIdInMemory relationshipId) {
        synchronized (outboundIds) {
            outboundIds.remove(relationshipId);
        }
        synchronized (inboundIds) {
            inboundIds.remove(relationshipId);
        }
    }

    public boolean isEmpty() {
        return outboundIds.isEmpty() && inboundIds.isEmpty();
    }

    @Override
    public String toString() {
        return "RelationshipsForNode{" +
                "outbound=" + outboundIds +
                ", inbound=" + inboundIds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipsForNode that = (RelationshipsForNode) o;
        return Objects.equals(outboundIds, that.outboundIds) && Objects.equals(inboundIds, that.inboundIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outboundIds, inboundIds);
    }
}
