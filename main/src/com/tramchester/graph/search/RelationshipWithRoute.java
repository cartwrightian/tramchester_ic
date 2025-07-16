package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;

public class RelationshipWithRoute implements HasId<Route> {
    private final IdFor<Route> routeId;
    private final ImmutableGraphRelationship relationship;

    public RelationshipWithRoute(final ImmutableGraphRelationship relationship) {
        this.routeId = relationship.getRouteId();
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
