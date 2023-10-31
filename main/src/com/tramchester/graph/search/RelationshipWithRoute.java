package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.facade.GraphRelationship;

public class RelationshipWithRoute implements HasId<Route> {
    private final GraphRelationship relationship;
    private final IdFor<Route> routeId;

    public RelationshipWithRoute(GraphRelationship relationship) {
        routeId = relationship.getRouteId();
        this.relationship = relationship;
    }

    public GraphRelationship getRelationship() {
        return relationship;
    }

    @Override
    public IdFor<Route> getId() {
        return routeId;
    }
}
