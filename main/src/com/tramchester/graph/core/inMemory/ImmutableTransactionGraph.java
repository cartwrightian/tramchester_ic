package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.stream.Stream;

public class ImmutableTransactionGraph implements Graph {
    private final ImmutableGraph underlying;

    public ImmutableTransactionGraph(final Graph underlying) {
        this.underlying = underlying;
    }

    @Override
    public void commit(GraphTransaction owningTransaction) {
        throw new RuntimeException("Got unexpected commit for " + owningTransaction);
    }

    @Override
    public GraphNodeInMemory createNode(final ImmutableEnumSet<GraphLabel> labels) {
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
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(final NodeIdInMemory id, final GraphDirection direction, final ImmutableEnumSet<TransportRelationshipTypes> types) {
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
    public Stream<GraphRelationship> findRelationshipsImmutableFor(final NodeIdInMemory id, final GraphDirection direction,
                                                                   final ImmutableEnumSet<TransportRelationshipTypes> types) {
        return underlying.findRelationshipsImmutableFor(id, direction, types);
    }

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
    public Stream<GraphRelationship> findRelationships(final TransportRelationshipTypes type) {
        return underlying.findRelationships(type);
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


    @Override
    public void close(GraphTransaction owningTransaction) {
        // NO-OP
    }

    @Override
    public Stream<GraphNodeInMemory> getUpdatedNodes() {
        return Stream.empty();
    }

    @Override
    public Stream<GraphRelationshipInMemory> getUpdatedRelationships() {
        return Stream.empty();
    }

    private static class ImmutableGraphException extends RuntimeException {
        ImmutableGraphException() {
            super("Not implemented for ImmutableGraph");
        }
    }
}
