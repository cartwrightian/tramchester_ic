package com.tramchester.graph.caches;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.time.Duration;
import java.util.EnumSet;

// KEEP for assisting with debugging
@SuppressWarnings("unused")
public class NodeContentsDirect implements NodeContentsRepository {

    @Override
    public IdFor<RouteStation> getRouteStationId(GraphNode node) {
        return node.getRouteStationId();
    }

    @Override
    public IdFor<Service> getServiceId(GraphNode node) {
        return node.getServiceId();
    }

    @Override
    public IdFor<Trip> getTripId(GraphNode node) {
        return node.getTripId();
    }

    @Override
    public TramTime getTime(GraphNode node) {
        return node.getTime();
    }

    @Override
    public IdFor<Trip> getTripId(GraphRelationship relationship) {
        return relationship.getTripId();
    }

    @Override
    public Duration getCost(GraphRelationship relationship) {
        return relationship.getCost();
    }

    @Override
    public void deleteFromCostCache(GraphRelationship relationship) {
        // no-op
    }

    @Override
    public EnumSet<GraphLabel> getLabels(GraphNode node) {
        return node.getLabels();
    }
}
