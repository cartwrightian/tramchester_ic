package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationshipId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class NodesAndEdges {
    private final ConcurrentMap<GraphRelationshipId, GraphRelationshipInMemory> relationships;
    private final ConcurrentMap<GraphNodeId, GraphNodeInMemory> nodes;

    NodesAndEdges() {
        relationships = new ConcurrentHashMap<>();
        nodes = new ConcurrentHashMap<>();
    }

    @JsonProperty(value = "nodes", access = JsonProperty.Access.READ_ONLY)
    public Set<GraphNodeInMemory> getNodes() {
        return new HashSet<>(nodes.values());
    }

    @JsonProperty(value = "relationships", access = JsonProperty.Access.READ_ONLY)
    public Set<GraphRelationshipInMemory> getRelationships() {
        return new HashSet<>(relationships.values());
    }

    public void clear() {
        relationships.clear();
        nodes.clear();
    }

    public void addNode(GraphNodeId id, GraphNodeInMemory node) {
        nodes.put(id, node);
    }

    public void addRelationship(final GraphRelationshipId id, final GraphRelationshipInMemory relationship) {
        relationships.put(id, relationship);
    }

    public boolean hasNode(final GraphNodeId id) {
        return nodes.containsKey(id);
    }

    public boolean hasRelationship(final GraphRelationshipId id) {
        return relationships.containsKey(id);
    }

    public GraphRelationshipInMemory getRelationship(GraphRelationshipId id) {
        return relationships.get(id);
    }

    public GraphNodeInMemory getNode(GraphNodeId id) {
        return nodes.get(id);
    }

    public void removeNode(GraphNodeId id) {
        nodes.remove(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NodesAndEdges that = (NodesAndEdges) o;
        return Objects.equals(relationships, that.relationships) && Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationships, nodes);
    }

    @Override
    public String toString() {
        return "NodesAndEdges{" +
                "relationships=" + relationships +
                ", nodes=" + nodes +
                '}';
    }

    Stream<GraphRelationshipInMemory> getOutbounds(final RelationshipsForNode relationshipsForNode) {
        return relationshipsForNode.getOutbound(relationships);
    }

    Stream<GraphRelationshipInMemory> getInbounds(final RelationshipsForNode relationshipsForNode) {
        return relationshipsForNode.getInbound(relationships);
    }
}
