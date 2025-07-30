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
import com.tramchester.graph.TransportRelationshipTypes;

import java.time.Duration;
import java.util.EnumSet;

public interface ImmutableGraphRelationship extends GraphRelationship {
    GraphRelationshipId getId();

    TramTime getTime();

    int getHour();

    Duration getCost();

    EnumSet<TransportMode> getTransportModes();

    TransportRelationshipTypes getType();

    IdFor<Route> getRouteId();

    IdFor<Service> getServiceId();

    IdFor<Trip> getTripId();

    boolean isType(TransportRelationshipTypes transportRelationshipType);

    IdFor<RouteStation> getRouteStationId();

    boolean validOn(TramDate tramDate);

    IdFor<Station> getStationId();

    int getStopSeqNumber();

    IdFor<Station> getEndStationId();

    IdFor<Station> getStartStationId();

    IdFor<StationLocalityGroup> getStationGroupId();

    IdSet<Trip> getTripIds();

    DateRange getDateRange();

    TimeRange getTimeRange();

    DateTimeRange getDateTimeRange();

    TramTime getStartTime();

    TramTime getEndTime();

    LocationId<?> getLocationId();

    boolean isNode();

    boolean isRelationship();

    boolean hasTripIdInList(IdFor<Trip> tripId);
}
