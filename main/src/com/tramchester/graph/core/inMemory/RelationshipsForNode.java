package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class RelationshipsForNode {
    private final Set<RelationshipIdInMemory> outboundIds;
    private final Set<RelationshipIdInMemory> inboundIds;
    private final ConcurrentMap<RelationshipIdInMemory, TransportRelationshipTypes> typeMap;

    private static final RelationshipsForNode empty = new RelationshipsForNode();

    RelationshipsForNode() {
        outboundIds = new HashSet<>();
        inboundIds = new HashSet<>();
        typeMap = new ConcurrentHashMap<>();
    }

    public static RelationshipsForNode empty() {
        return empty;
    }

    public Stream<GraphRelationshipInMemory>  getOutbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source) {
        synchronized (outboundIds) {
            return outboundIds.stream().map(source::get);
        }
    }

    public Stream<GraphRelationshipInMemory> getOutbound(final ConcurrentMap<RelationshipIdInMemory, GraphRelationshipInMemory> source,
                                                         final ImmutableEnumSet<TransportRelationshipTypes> types) {
        synchronized (outboundIds) {
            return outboundIds.stream().filter(id -> types.contains(typeMap.get(id))).map(source::get);
        }
    }

    public Stream<GraphRelationshipInMemory> getInbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source) {
        synchronized (inboundIds) {
            return inboundIds.stream().map(source::get);
        }
    }

    public Stream<GraphRelationshipInMemory> getInbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source,
                                                        final ImmutableEnumSet<TransportRelationshipTypes> types) {
        synchronized (inboundIds) {
            return inboundIds.stream().filter(id -> types.contains(typeMap.get(id))).map(source::get);
        }
    }

    /***
     * Add a outbound relationship
     * @param relationshipId the id of relationship to add
     * @return true if added new outbound
     */
    public boolean putOutbound(final RelationshipIdInMemory relationshipId, final TransportRelationshipTypes relationshipType) {
        typeMap.put(relationshipId, relationshipType);
        synchronized (outboundIds) {
            return outboundIds.add(relationshipId);
        }
    }

    /***
     * Add a inbound relationship
     * @param relationshipId the id of relationship to add
     * @return true if added new inbound
     */
    public boolean putInbound(final RelationshipIdInMemory relationshipId, final TransportRelationshipTypes relationshipType) {
        typeMap.put(relationshipId, relationshipType);
        synchronized (inboundIds) {
            return inboundIds.add(relationshipId);
        }
    }

    public boolean remove(final RelationshipIdInMemory relationshipId) {
        boolean flag;
        typeMap.remove(relationshipId);
        synchronized (outboundIds) {
            flag = outboundIds.remove(relationshipId);
        }
        synchronized (inboundIds) {
            flag = flag || inboundIds.remove(relationshipId);
        }
        return flag;
    }

    public synchronized boolean isEmpty() {
        return outboundIds.isEmpty() && inboundIds.isEmpty();
    }

    @Override
    public String toString() {
        return "RelationshipsForNode{" +
                "outbound=" + outboundIds +
                ", inbound=" + inboundIds +
                ", typeMap=" + typeMap +
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

    public synchronized Stream<RelationshipIdInMemory> getRelationshipIds() {
        return Stream.concat(outboundIds.stream(), inboundIds.stream());
    }


}
