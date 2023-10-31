package com.tramchester.graph.caches;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.time.Duration;
import java.util.EnumSet;

@ImplementedBy(CachedNodeOperations.class)
public interface NodeContentsRepository  {

    IdFor<RouteStation> getRouteStationId(GraphNode node);
    IdFor<Service> getServiceId(GraphNode node);
    IdFor<Trip> getTripId(GraphNode node);

    TramTime getTime(GraphNode node);
    int getHour(GraphNode node);

    IdFor<Trip> getTripId(GraphRelationship relationship);
    Duration getCost(GraphRelationship lastRelationship);
    void deleteFromCostCache(GraphRelationship relationship);

    EnumSet<GraphLabel> getLabels(GraphNode node);

}
