package com.tramchester.graph.facade;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TransportRelationshipTypes;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;

public interface GraphRelationship {

    GraphRelationshipId getId();

    TramTime getTime();

    int getHour();

    Duration getCost();

    GraphNode getEndNode(final GraphTransaction txn);

    GraphNode getStartNode(GraphTransaction txn);

    GraphNodeId getStartNodeId(GraphTransaction txn);

    GraphNodeId getEndNodeId(GraphTransaction txn);

    EnumSet<TransportMode> getTransportModes() ;

    TransportRelationshipTypes getType();

    IdFor<Route> getRouteId();

    IdFor<Service> getServiceId();

    IdFor<Trip> getTripId();

    boolean isType(TransportRelationshipTypes transportRelationshipType);

    IdFor<RouteStation> getRouteStationId();

    Map<String,Object> getAllProperties();

    boolean isDayOffset();

    boolean validOn(TramDate tramDate);

    IdFor<Station> getStationId();

    boolean hasProperty(GraphPropertyKey graphPropertyKey);

    int getStopSeqNumber();

    IdFor<Station> getEndStationId();

    IdFor<Station> getStartStationId();

    IdFor<StationLocalityGroup> getStationGroupId();

    IdSet<Trip> getTripIds();

    boolean hasTripIdInList(IdFor<Trip> tripId);

    DateRange getDateRange();

    TimeRange getTimeRange();

    DateTimeRange getDateTimeRange();

    TramTime getStartTime();

    TramTime getEndTime();
}
