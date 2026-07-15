package com.tramchester.graph.core;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.reference.TransportRelationshipTypes;

public interface GraphRelationship extends GraphEntity<GraphRelationshipId> {

    GraphRelationshipId getId();
    GraphNode getEndNode(final GraphTransaction txn);
    GraphNode getStartNode(GraphTransaction txn);
    GraphNodeId getStartNodeId(GraphTransaction txn);
    GraphNodeId getEndNodeId(GraphTransaction txn);

    boolean hasProperty(GraphPropertyKey graphPropertyKey);

    IdFor<Station> getEndStationId(GraphTransaction txn);
    IdFor<Station> getStartStationId(GraphTransaction txn);
    IdFor<StationLocalityGroup> getStationGroupId(GraphTransaction txn);
    LocationId<?> getLocationId(GraphTransaction txn);

    TramTime getTime();
    int getHour();
    TramDuration getCost();
    ImmutableEnumSet<TransportMode> getTransportModes() ;
    TransportRelationshipTypes getType();
    IdFor<Route> getRouteId();
    IdFor<Service> getServiceId();
    IdFor<Trip> getTripId();
    IdFor<RouteStation> getRouteStationId();
    IdFor<Station> getStationId();
    int getStopSeqNumber();
    ImmutableIdSet<Trip> getTripIds();
    DateRange getDateRange();
    TimeRange getTimeRange();
    DateTimeRange getDateTimeRange();
    TramTime getStartTime();
    TramTime getEndTime();

    boolean isType(TransportRelationshipTypes transportRelationshipType);
    boolean validOn(TramDate tramDate);
    boolean hasTripIdInList(IdFor<Trip> tripId);

}
