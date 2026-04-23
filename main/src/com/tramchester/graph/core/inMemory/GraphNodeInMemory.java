package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.GraphLabels;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.DAY_OFFSET;

public class GraphNodeInMemory extends GraphNodeProperties<PropertyContainer> {

    private final NodeIdInMemory id;
    private final AtomicInteger dirtyCount;

    // push labels into graph, so can do the validation check etc
    private final GraphNodeLabelsContainer labelsContainer;

    public GraphNodeInMemory(final NodeIdInMemory id, final GraphLabels labelsContainer, final boolean diagnostics) {
        this(new PropertyContainer(diagnostics), id, labelsContainer);
    }

    @JsonCreator
    public GraphNodeInMemory(
            @JsonProperty("nodeId") final NodeIdInMemory id,
            @JsonProperty("labels") final GraphLabels labels,
            @JsonProperty("properties") List<PropertyDTO> properties) {
        this(new PropertyContainer(properties), id, labels);
    }

    private GraphNodeInMemory(final PropertyContainer propertyContainer, final NodeIdInMemory id, final GraphLabels labelsContainer) {
        super(propertyContainer);
        this.id = id;
        this.labelsContainer = new GraphNodeLabelsContainer(this, labelsContainer);
        dirtyCount = new AtomicInteger(0);
    }

    public GraphNodeInMemory copy() {
        return new GraphNodeInMemory(super.copyProperties(), id, labelsContainer.getLabels());
    }

    @Override
    protected void invalidateCache() {
        dirtyCount.getAndIncrement();
    }

    @JsonIgnore
    public boolean isDirty() {
        return dirtyCount.get()>0;
    }


    public void setClean() {
        dirtyCount.set(0);
    }

    @JsonGetter("properties")
    public List<PropertyDTO> getProperties() {
        return getAllProperties().entrySet().stream().
                filter(entry -> entry.getKey()!=DAY_OFFSET).
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
                ", labels=" + labelsContainer +
                '}';
    }

    @JsonProperty(value = "nodeId")
    @Override
    public NodeIdInMemory getId() {
        return id;
    }

    @Override
    public boolean hasLabel(GraphLabel graphLabel) {
        return labelsContainer.contains(graphLabel);
    }

    @JsonIgnore
    @Override
    public GraphLabels getLabels() {
        return labelsContainer.getLabels();
    }

    @JsonProperty(value = "labels")
    GraphLabels getLabelsForSerialization() {
        return labelsContainer.getLabels();
    }

    @Override
    public boolean hasRelationship(final GraphTransaction txn, final GraphDirection direction,
                                   final TransportRelationshipTypes transportRelationshipTypes) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.hasRelationship(id, direction, transportRelationshipTypes);
    }

    @Override
    public GraphRelationship getSingleRelationship(final GraphTransaction txn, final TransportRelationshipTypes transportRelationshipTypes,
                                                   final GraphDirection direction) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getSingleRelationshipImmutable(id, direction, transportRelationshipTypes);
    }

    @Override
    public Stream<GraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                      final TransportRelationshipTypes relationshipType) {
        return getRelationships(txn, direction, relationshipType.singleton());
    }

    @Override
    public Stream<GraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                      final ImmutableEnumSet<TransportRelationshipTypes> types) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getRelationshipImmutable(id, direction, types);
    }

    @Override
    public Stream<GraphRelationship> getAllRelationships(final GraphTransaction txn, final GraphDirection direction) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;

        return inMemory.findRelationships(id, direction);
    }
    
    @Override
    public boolean hasRelationship(final GraphTransaction txn, final GraphDirection graphDirection,
                                   final TransportRelationshipTypes relationshipType, final GraphNode end) {
        return getRelationships(txn, graphDirection, relationshipType).anyMatch(rel -> rel.getEndNode(txn).equals(end));
    }

    @Override
    public MutableGraphRelationship getSingleRelationshipMutable(final MutableGraphTransaction txn, final TransportRelationshipTypes transportRelationshipTypes,
                                                                 final GraphDirection graphDirection, final GraphNode end) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getRelationshipMutable(id, graphDirection, transportRelationshipTypes.singleton(), end);
    }

    @Override
    public synchronized void delete(final MutableGraphTransaction txn) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        inMemory.delete(id);
        invalidateCache();
    }

    @Override
    public MutableGraphRelationship createRelationshipTo(final MutableGraphTransaction txn,
                                                         final MutableGraphNode end, final TransportRelationshipTypes relationshipType) {
        return txn.createRelationship(this, end, relationshipType);
    }

    @Override
    public synchronized void addLabel(final MutableGraphTransaction txn, final GraphLabel label) {
        final GraphTransactionInMemory inMemoryTxn = (GraphTransactionInMemory) txn;
        //labels.add(label);
        labelsContainer.add(txn, label);
        // update labels to nodes mapping in GraphCore
        inMemoryTxn.addLabel(id, label);
    }

    @Override
    public MutableGraphRelationship getSingleRelationshipMutable(final MutableGraphTransaction txn, final TransportRelationshipTypes transportRelationshipTypes,
                                                                 final GraphDirection direction) {
        GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        return inMemory.getSingleRelationshipMutable(id, direction, transportRelationshipTypes);
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
