package com.tramchester.graph.core;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.time.Duration;
import java.util.EnumSet;


// TODO just use GraphRelationship

@Deprecated
public interface ImmutableGraphRelationship extends GraphRelationship {

//    @Override
//    GraphRelationshipId getId();
//
//    @Override
//    TramTime getTime();
//
//    @Override
//    int getHour();
//
//    @Override
//    Duration getCost();
//
//    @Override
//    EnumSet<TransportMode> getTransportModes();
//
//    @Override
//    TransportRelationshipTypes getType();
//
//    @Override
//    IdFor<Route> getRouteId();
//
//    @Override
//    IdFor<Service> getServiceId();
//
//    @Override
//    IdFor<Trip> getTripId();
//
//    @Override
//    boolean isType(TransportRelationshipTypes transportRelationshipType);
//
//    @Override
//    IdFor<RouteStation> getRouteStationId();
//
//    @Override
//    boolean validOn(TramDate tramDate);
//
//    @Override
//    IdFor<Station> getStationId();
//
//    @Override
//    int getStopSeqNumber();
//
//    @Override
//    IdFor<Station> getEndStationId();
//
//    @Override
//    IdFor<Station> getStartStationId();
//
//    @Override
//    IdFor<StationLocalityGroup> getStationGroupId();
//
//    @Override
//    IdSet<Trip> getTripIds();
//
//    @Override
//    DateRange getDateRange();
//
//    @Override
//    TimeRange getTimeRange();
//
//    @Override
//    DateTimeRange getDateTimeRange();
//
//    @Override
//    TramTime getStartTime();
//
//    @Override
//    TramTime getEndTime();
//
//    @Override
//    LocationId<?> getLocationId();
//
//    @Override
//    boolean isNode();
//
//    @Override
//    boolean isRelationship();
//
//    @Override
//    boolean hasTripIdInList(IdFor<Trip> tripId);
}
