package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.Objects;

public class GraphRelationshipInMemory extends GraphRelationshipProperties<PropertyContainer> {
    private final TransportRelationshipTypes relationshipType;
    private final GraphRelationshipId id;
    private final MutableGraphNode start;
    private final MutableGraphNode end;

    public GraphRelationshipInMemory(TransportRelationshipTypes relationshipType, GraphRelationshipId id,
                                     MutableGraphNode start, MutableGraphNode end) {
        super(new PropertyContainer());
        this.relationshipType = relationshipType;
        this.id = id;
        this.start = start;
        this.end = end;
    }

    @Override
    public void delete(MutableGraphTransaction txn) {
        GraphTransactionInMemory inMemory = (GraphTransactionInMemory) txn;
        inMemory.delete(id);
    }

    @Override
    protected void invalidateCache() {
        // no-op
    }

    @Override
    public GraphRelationshipId getId() {
        return id;
    }

    @Override
    public GraphNode getEndNode(GraphTransaction txn) {
        return end;
    }

    @Override
    public GraphNode getStartNode(GraphTransaction txn) {
        return start;
    }

    @Override
    public GraphNodeId getStartNodeId(GraphTransaction txn) {
        return getStartId();
    }

    GraphNodeId getStartId() {
        return start.getId();
    }

    @Override
    public GraphNodeId getEndNodeId(GraphTransaction txn) {
        return getEndId();
    }

    GraphNodeId getEndId() {
        return end.getId();
    }

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
    public IdFor<Station> getEndStationId() {
        return end.getStationId();
    }

    @JsonIgnore
    @Override
    public IdFor<Station> getStartStationId() {
        return start.getStationId();
    }

    @JsonIgnore
    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return end.getStationGroupId();
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
                ", start=" + start.getId() +
                ", end=" + end.getId() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelationshipInMemory that = (GraphRelationshipInMemory) o;
        return relationshipType == that.relationshipType && Objects.equals(id, that.id)
                && Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationshipType, id, start, end);
    }
}
