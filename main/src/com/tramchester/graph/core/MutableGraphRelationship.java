package com.tramchester.graph.core;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.time.LocalDate;

public interface MutableGraphRelationship extends GraphRelationship {

    void delete();

    void setTransportMode(TransportMode transportMode);

    void setTime(TramTime tramTime);

    void setHour(int hour);

    void setCost(Duration cost);

    <C extends GraphProperty & CoreDomain & HasId<C>> void set(C domainItem);

    void setRouteStationId(IdFor<RouteStation> routeStationId);

    void setStopSeqNum(int sequenceNumber);

    void setDateTimeRange(DateTimeRange dateTimeRange);

    void setDateRange(DateRange range);

    void setTimeRange(TimeRange timeRange);

    void setStartTime(TramTime tramTime);

    void setEndTime(TramTime tramTime);

    void setEndDate(LocalDate localDate);

    void setStartDate(LocalDate localDate);

    void addTransportMode(TransportMode mode);

    void addTripId(IdFor<Trip> tripId);

}
