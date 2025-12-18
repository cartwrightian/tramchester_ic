package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.DAY_OFFSET;

public class GraphNodeInMemory extends GraphNodeProperties<PropertyContainer> {

    private final NodeIdInMemory id;

    // push labels into graph, so can do the validation check etc
    private final EnumSet<GraphLabel> labels;

    public GraphNodeInMemory(final NodeIdInMemory id, final EnumSet<GraphLabel> labels) {
        super(new PropertyContainer());
        this.id = id;
        this.labels = labels;
    }

    @JsonCreator
    public GraphNodeInMemory(
            @JsonProperty("nodeId") final NodeIdInMemory id,
            @JsonProperty("labels") final EnumSet<GraphLabel> labels,
            @JsonProperty("properties") List<PropertyDTO> properties) {
        super(new PropertyContainer(properties));
        this.id = id;
        this.labels = labels;
    }

    @JsonGetter("properties")
    public List<PropertyDTO> getProperties() {
        return getAllProperties().entrySet().stream().
                filter(entry -> !entry.getKey().equals(DAY_OFFSET.getText())).
                map(PropertyDTO::fromMapEntry).toList();
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

    @JsonProperty(value = "nodeId")
    @Override
    public NodeIdInMemory getId() {
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
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.hasRelationship(id, direction, transportRelationshipTypes);
    }

    @Override
    public GraphRelationship getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, GraphDirection direction) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getSingleRelationship(id, direction, transportRelationshipTypes);
    }

    @Override
    public Stream<GraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction, final TransportRelationshipTypes relationshipType) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getRelationships(id, direction, EnumSet.of(relationshipType)).map(item -> item);
    }

    @Override
    public Stream<GraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, EnumSet<TransportRelationshipTypes> types) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getRelationships(id, direction, types).map(item -> item);
    }

    @Override
    public Stream<GraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction, final TransportRelationshipTypes... transportRelationshipTypes) {
        final List<TransportRelationshipTypes> list = Arrays.asList(transportRelationshipTypes);
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;

        if (list.isEmpty()) {
            return inMemory.getRelationships(id, direction);
        } else {
            final EnumSet<TransportRelationshipTypes> types = EnumSet.copyOf(list);
            return inMemory.getRelationships(id, direction, types).map(item -> item);
        }
    }

    @Override
    public synchronized void delete(final MutableGraphTransaction txn) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        inMemory.delete(id);
    }

    @Override
    public MutableGraphRelationship createRelationshipTo(final MutableGraphTransaction txn,
                                                         final MutableGraphNode end, final TransportRelationshipTypes relationshipType) {
        return txn.createRelationship(this, end, relationshipType);
    }

    @Override
    public synchronized void addLabel(final MutableGraphTransaction txn, final GraphLabel label) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        labels.add(label);
        inMemory.addLabel(id, label);
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
