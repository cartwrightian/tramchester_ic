package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;

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
    public void delete() {

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
        return start.getId();
    }

    @Override
    public GraphNodeId getEndNodeId(GraphTransaction txn) {
        return end.getId();
    }

    @Override
    public TransportRelationshipTypes getType() {
        return relationshipType;
    }

    @Override
    public boolean isType(TransportRelationshipTypes transportRelationshipType) {
        return false;
    }

    @Override
    public IdFor<Station> getEndStationId() {
        return end.getStationId();
    }

    @Override
    public IdFor<Station> getStartStationId() {
        return start.getStationId();
    }

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

}
