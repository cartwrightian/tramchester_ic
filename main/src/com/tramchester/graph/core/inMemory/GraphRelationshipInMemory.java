package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tramchester.graph.GraphPropertyKey.DAY_OFFSET;

public class GraphRelationshipInMemory extends GraphRelationshipProperties<PropertyContainer> {
    private final TransportRelationshipTypes relationshipType;
    private final RelationshipIdInMemory id;
    private final NodeIdInMemory startId;
    private final NodeIdInMemory endId;
    private final AtomicInteger dirtyCount;

    public GraphRelationshipInMemory(TransportRelationshipTypes relationshipType, RelationshipIdInMemory id,
                                     NodeIdInMemory startId, NodeIdInMemory endId) {
        this(new PropertyContainer(), relationshipType, id, startId, endId);
    }

    @JsonCreator
    public GraphRelationshipInMemory(
            @JsonProperty("relationshipType") TransportRelationshipTypes relationshipType,
            @JsonProperty("relationshipId") RelationshipIdInMemory id,
            @JsonProperty("startId") NodeIdInMemory startId,
            @JsonProperty("endId") NodeIdInMemory endId,
            @JsonProperty("properties") List<PropertyDTO> props) {
        this(new PropertyContainer(props), relationshipType, id, startId, endId);
    }

    private GraphRelationshipInMemory(PropertyContainer propertyContainer, TransportRelationshipTypes relationshipType,
                                     RelationshipIdInMemory id, NodeIdInMemory startId, NodeIdInMemory endId) {
        super(propertyContainer);
        this.relationshipType = relationshipType;
        this.id = id;
        this.startId = startId;
        this.endId = endId;
        dirtyCount = new AtomicInteger(0);
    }

    public GraphRelationshipInMemory copy() {
        return new GraphRelationshipInMemory(super.copyProperties(), relationshipType, id, startId, endId);
    }

    @Override
    public void delete(final MutableGraphTransaction txn) {
        final GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        inMemory.delete(id);
        invalidateCache();
    }

    @Override
    protected void invalidateCache() {
        dirtyCount.getAndIncrement();
    }

    @JsonIgnore
    public boolean isDirty() {
        return dirtyCount.get()>0;
    }

    @JsonGetter("properties")
    public List<PropertyDTO> getProperties() {
        return getAllProperties().entrySet().stream().
                filter(entry -> !entry.getKey().equals(DAY_OFFSET.getText())).
                map(PropertyDTO::fromMapEntry).toList();
    }

    @JsonProperty(value = "relationshipId")
    @Override
    public RelationshipIdInMemory getId() {
        return id;
    }

    @Override
    public GraphNode getEndNode(final GraphTransaction txn) {
        return txn.getNodeById(endId);
    }

    @Override
    public GraphNode getStartNode(final GraphTransaction txn) {
        return txn.getNodeById(startId);
    }

    @Override
    public GraphNodeId getStartNodeId(GraphTransaction txn) {
        return getStartId();
    }

    @JsonGetter("startId")
    public NodeIdInMemory getStartId() {
        return startId;
    }

    @Override
    public GraphNodeId getEndNodeId(GraphTransaction txn) {
        return getEndId();
    }

    @JsonGetter("endId")
    public NodeIdInMemory getEndId() {
        return endId;
    }

    @JsonProperty("relationshipType")
    @Override
    public TransportRelationshipTypes getType() {
        return relationshipType;
    }

    @Override
    public boolean isType(TransportRelationshipTypes transportRelationshipType) {
        return relationshipType.equals(transportRelationshipType);
    }

    @JsonIgnore
    @Override
    public IdFor<Station> getEndStationId(GraphTransaction txn) {
        return getEndNode(txn).getStationId();
    }

    @JsonIgnore
    @Override
    public IdFor<Station> getStartStationId(GraphTransaction txn) {
        return getStartNode(txn).getStationId();
    }

    @JsonIgnore
    @Override
    public IdFor<StationLocalityGroup> getStationGroupId(GraphTransaction txn) {
        return getEndNode(txn).getStationGroupId();
    }

    @Override
    public boolean isNode() {
        return false;
    }

    @Override
    public boolean isRelationship() {
        return true;
    }

    @Override
    public String toString() {
        return "GraphRelationshipInMemory{" +
                "relationshipType=" + relationshipType +
                ", id=" + id +
                ", start=" + startId +
                ", end=" + endId +
                ", props=" + getAllProperties() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelationshipInMemory that = (GraphRelationshipInMemory) o;
        return relationshipType == that.relationshipType && Objects.equals(id, that.id)
                && Objects.equals(startId, that.startId) && Objects.equals(endId, that.endId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationshipType, id, startId, endId);
    }


}
