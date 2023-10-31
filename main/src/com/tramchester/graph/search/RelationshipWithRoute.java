package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.graphbuild.GraphProps;

public class RelationshipWithRoute implements HasId<Route> {
    private final GraphRelationship relationship;
    private final IdFor<Route> routeId;

    public RelationshipWithRoute(GraphRelationship relationship) {
        routeId = GraphProps.getRouteIdFrom(relationship);
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
