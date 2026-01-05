package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.stream.Stream;

public class ImmutableGraph implements Graph {
    private final Graph underlying;

    public ImmutableGraph(Graph underlying) {
        this.underlying = underlying;
    }

    @Override
    public GraphNodeInMemory createNode(EnumSet<GraphLabel> labels) {
        throw new ImmutableGraphException();
    }

    @Override
    public GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType, GraphNodeInMemory begin, GraphNodeInMemory end) {
        throw new ImmutableGraphException();
    }

    @Override
    public void delete(RelationshipIdInMemory id) {
        throw new ImmutableGraphException();
    }

    @Override
    public void delete(NodeIdInMemory id) {
        throw new ImmutableGraphException();
    }

    @Override
    public void addLabel(NodeIdInMemory id, GraphLabel label) {
        throw new ImmutableGraphException();
    }

    @Override
    public GraphNodeInMemory getNodeMutable(NodeIdInMemory nodeId) {
        throw new ImmutableGraphException();
    }

    @Override
    public GraphRelationshipInMemory getSingleRelationshipMutable(NodeIdInMemory id, GraphDirection direction, TransportRelationshipTypes transportRelationshipType) {
        throw new ImmutableGraphException();
    }

    @Override
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(NodeIdInMemory id, GraphDirection direction) {
        throw new ImmutableGraphException();
    }

    @Override
    public Stream<GraphNodeInMemory> findNodesMutable(GraphLabel graphLabel) {
        throw new ImmutableGraphException();
    }

    // immutable

    @Override
    public Stream<GraphNode> findNodesImmutable(final GraphLabel graphLabel) {
        return underlying.findNodesImmutable(graphLabel);
    }

    @Override
    public Stream<GraphNode> findNodesImmutable(final GraphLabel label, final GraphPropertyKey key, final String value) {
        return underlying.findNodesImmutable(label, key, value);
    }

    @Override
    public GraphNode getNodeImmutable(final NodeIdInMemory nodeId) {
        return underlying.getNodeImmutable(nodeId);
    }

    @Override
    public Stream<GraphRelationship> findRelationshipsImmutableFor(final NodeIdInMemory id, final GraphDirection direction) {
        return underlying.findRelationshipsImmutableFor(id, direction);
    }

    @Override
    public GraphRelationship getRelationship(final RelationshipIdInMemory graphRelationshipId) {
        return underlying.getRelationship(graphRelationshipId);
    }

    @Override
    public long getNumberOf(TransportRelationshipTypes relationshipType) {
        return underlying.getNumberOf(relationshipType);
    }

    private static class ImmutableGraphException extends RuntimeException {
        ImmutableGraphException() {
            super("Not implemented for ImmutableGraph");
        }
    }
}
