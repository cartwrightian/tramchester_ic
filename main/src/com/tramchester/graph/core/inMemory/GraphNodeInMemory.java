package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class GraphNodeInMemory extends GraphNodeProperties<PropertyContainer> {

    private final GraphNodeId id;
    private final EnumSet<GraphLabel> labels;

    public GraphNodeInMemory(final GraphNodeId id, final EnumSet<GraphLabel> labels) {
        super(new PropertyContainer());
        this.id = id;
        this.labels = labels;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphNodeInMemory that = (GraphNodeInMemory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "GraphNodeInMemory{" +
                "id=" + id +
                ", labels=" + labels +
                '}';
    }
    
    @Override
    protected void invalidateCache() {
        // no-op
    }

    @Override
    public GraphNodeId getId() {
        return id;
    }

    @Override
    public boolean hasLabel(GraphLabel graphLabel) {
        return labels.contains(graphLabel);
    }

    @Override
    public EnumSet<GraphLabel> getLabels() {
        return labels;
    }

    @Override
    public boolean hasRelationship(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes transportRelationshipTypes) {
        GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.hasRelationship(id, direction, transportRelationshipTypes);
    }

    @Override
    public ImmutableGraphRelationship getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, GraphDirection direction) {
        GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getSingleRelationship(id, direction, transportRelationshipTypes);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction, final TransportRelationshipTypes relationshipType) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getRelationships(id, direction, EnumSet.of(relationshipType)).map(item -> item);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction, final TransportRelationshipTypes... transportRelationshipTypes) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        final List<TransportRelationshipTypes> list = Arrays.asList(transportRelationshipTypes);
        final EnumSet<TransportRelationshipTypes> types = EnumSet.copyOf(list);

        return inMemory.getRelationships(id, direction, types).map(item -> item);
    }

    @Override
    public void delete() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public MutableGraphRelationship createRelationshipTo(MutableGraphTransaction txn,
                                                         MutableGraphNode end, TransportRelationshipTypes relationshipType) {
        return txn.createRelationship(this, end, relationshipType);
    }

    @Override
    public void addLabel(final GraphLabel label) {
        labels.add(label);
    }

    @Override
    public Stream<MutableGraphRelationship> getRelationshipsMutable(MutableGraphTransaction txn, GraphDirection direction, TransportRelationshipTypes relationshipType) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getRelationships(id, direction, EnumSet.of(relationshipType)).map(item -> item);
    }

    @Override
    public MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, GraphDirection direction) {
        GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getSingleRelationship(id, direction, transportRelationshipTypes);
    }


    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isRelationship() {
        return false;
    }
}
