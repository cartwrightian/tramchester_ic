package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class RelationshipsForNode {
    private final ConcurrentMap<TransportRelationshipTypes, Set<RelationshipIdInMemory>> outboundIds;
    private final ConcurrentMap<TransportRelationshipTypes, Set<RelationshipIdInMemory>> inboundIds;

    //private final ConcurrentMap<RelationshipIdInMemory, TransportRelationshipTypes> typeMap;

    private static final RelationshipsForNode empty = new RelationshipsForNode();

    RelationshipsForNode() {
        outboundIds = new ConcurrentHashMap<>();
        inboundIds = new ConcurrentHashMap<>();
        //typeMap = new ConcurrentHashMap<>();
    }

    public static RelationshipsForNode empty() {
        return empty;
    }

    public Stream<GraphRelationshipInMemory>  getOutbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source) {
        synchronized (outboundIds) {
            return getOutbounds().map(source::get);
            //return outboundIds.stream().map(source::get);
        }
    }

    private @NotNull Stream<RelationshipIdInMemory> getOutbounds() {
        return outboundIds.values().stream().flatMap(Collection::stream);
    }

    public Stream<GraphRelationshipInMemory> getOutbound(final ConcurrentMap<RelationshipIdInMemory, GraphRelationshipInMemory> source,
                                                         final ImmutableEnumSet<TransportRelationshipTypes> types) {
        synchronized (outboundIds) {
            return types.stream().
                    filter(outboundIds::containsKey).
                    flatMap(type -> outboundIds.get(type).stream()).
                    map(source::get);
            //return outboundIds.stream().filter(id -> types.contains(typeMap.get(id))).map(source::get);
        }
    }

    public Stream<GraphRelationshipInMemory> getInbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source) {
        synchronized (inboundIds) {
            return getInbounds().map(source::get);
            //return inboundIds.stream().map(source::get);
        }
    }

    private @NotNull Stream<RelationshipIdInMemory> getInbounds() {
        return inboundIds.values().stream().flatMap(Collection::stream);
    }

    public Stream<GraphRelationshipInMemory> getInbound(final Map<RelationshipIdInMemory, GraphRelationshipInMemory> source,
                                                        final ImmutableEnumSet<TransportRelationshipTypes> types) {
        synchronized (inboundIds) {
            return types.stream().
                    filter(inboundIds::containsKey).
                    flatMap(type -> inboundIds.get(type).stream()).
                    map(source::get);
            //return inboundIds.stream().filter(id -> types.contains(typeMap.get(id))).map(source::get);
        }
    }

    /***
     * Add a outbound relationship
     * @param relationshipId the id of relationship to add
     * @return true if added new outbound
     */
    public boolean putOutbound(final RelationshipIdInMemory relationshipId, final TransportRelationshipTypes relationshipType) {
        //typeMap.put(relationshipId, relationshipType);
        synchronized (outboundIds) {
            if (!outboundIds.containsKey(relationshipType)) {
                outboundIds.put(relationshipType, new HashSet<>());
            }
            return outboundIds.get(relationshipType).add(relationshipId);
            //return outboundIds.add(relationshipId);
        }
    }

    /***
     * Add a inbound relationship
     * @param relationshipId the id of relationship to add
     * @return true if added new inbound
     */
    public boolean putInbound(final RelationshipIdInMemory relationshipId, final TransportRelationshipTypes relationshipType) {
        //typeMap.put(relationshipId, relationshipType);
        synchronized (inboundIds) {
            if (!inboundIds.containsKey(relationshipType)) {
                inboundIds.put(relationshipType, new HashSet<>());
            }
            return inboundIds.get(relationshipType).add(relationshipId);
            //return inboundIds.add(relationshipId);
        }
    }

    public boolean remove(final RelationshipIdInMemory relationshipId, final TransportRelationshipTypes type) {
        boolean flag = false;
        //typeMap.remove(relationshipId);
        synchronized (outboundIds) {
            if (outboundIds.containsKey(type)) {
                flag = outboundIds.get(type).remove(relationshipId);
                if (outboundIds.get(type).isEmpty()) {
                    outboundIds.remove(type);
                }
            }
        }
        synchronized (inboundIds) {
            if (inboundIds.containsKey(type)) {
                flag = flag || inboundIds.get(type).remove(relationshipId);
                if (inboundIds.get(type).isEmpty()) {
                    inboundIds.remove(type);
                }
            }
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
                //", typeMap=" + typeMap +
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
        return Stream.concat(getOutbounds(), getInbounds());
    }


}
