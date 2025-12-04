package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodesAndEdges {
    private final ConcurrentMap<RelationshipIdInMemory, GraphRelationshipInMemory> relationships;
    private final ConcurrentMap<NodeIdInMemory, GraphNodeInMemory> nodes;

    NodesAndEdges() {
        relationships = new ConcurrentHashMap<>();
        nodes = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public NodesAndEdges(
            @JsonProperty(value = "relationships", required = true) Set<GraphRelationshipInMemory> relationshipSet,
            @JsonProperty(value = "nodes", required = true) Set<GraphNodeInMemory> nodeSet) {
        relationships = new ConcurrentHashMap<>();
        nodes = new ConcurrentHashMap<>();

        Map<RelationshipIdInMemory, GraphRelationshipInMemory> relationshipMap = relationshipSet.stream().
                collect(Collectors.toMap(GraphRelationshipInMemory::getId, rel -> rel));
        relationships.putAll(relationshipMap);

        Map<NodeIdInMemory, GraphNodeInMemory> nodeMap = nodeSet.stream().
                collect(Collectors.toMap(GraphNodeInMemory::getId, node -> node));
        nodes.putAll(nodeMap);
    }

    @JsonProperty(value = "nodes")
    public Set<GraphNodeInMemory> getNodes() {
        return new HashSet<>(nodes.values());
    }

    @JsonProperty(value = "relationships")
    public Set<GraphRelationshipInMemory> getRelationships() {
        return new HashSet<>(relationships.values());
    }

    public void clear() {
        relationships.clear();
        nodes.clear();
    }

    public void addNode(NodeIdInMemory id, GraphNodeInMemory node) {
        nodes.put(id, node);
    }

    public void addRelationship(final RelationshipIdInMemory id, final GraphRelationshipInMemory relationship) {
        relationships.put(id, relationship);
    }

    public boolean hasNode(final NodeIdInMemory id) {
        return nodes.containsKey(id);
    }

    public boolean hasRelationship(final RelationshipIdInMemory id) {
        return relationships.containsKey(id);
    }

    public GraphRelationshipInMemory getRelationship(RelationshipIdInMemory id) {
        return relationships.get(id);
    }

    public GraphNodeInMemory getNode(NodeIdInMemory id) {
        return nodes.get(id);
    }

    public void removeNode(NodeIdInMemory id) {
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

    public void updateHighestId(AtomicInteger toUpdate) {
        Optional<NodeIdInMemory> max = nodes.keySet().stream().max(Comparable::compareTo);
        if (max.isEmpty()) {
            throw new RuntimeException("Unable to find max node id");
        } else {
            NodeIdInMemory found = max.get();
            found.recordIdTo(toUpdate);
        }
    }
}
