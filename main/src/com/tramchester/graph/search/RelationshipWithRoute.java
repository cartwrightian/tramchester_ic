package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.facade.ImmutableGraphRelationship;

public class RelationshipWithRoute implements HasId<Route> {
    private final ImmutableGraphRelationship relationship;
    private final IdFor<Route> routeId;

    public RelationshipWithRoute(ImmutableGraphRelationship relationship) {
        routeId = relationship.getRouteId();
        this.relationship = relationship;
    }

    public ImmutableGraphRelationship getRelationship() {
        return relationship;
    }

    @Override
    public IdFor<Route> getId() {
        return routeId;
    }
}
