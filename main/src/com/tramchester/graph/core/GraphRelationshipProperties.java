package com.tramchester.graph.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;

import static com.tramchester.graph.GraphPropertyKey.*;
import static com.tramchester.graph.reference.TransportRelationshipTypes.*;

public abstract class GraphRelationshipProperties <T extends GraphEntityProperties.GraphProps>
        extends GraphEntityProperties<T> implements MutableGraphRelationship {

    private static final EnumSet<TransportRelationshipTypes> HAS_STATION_ID = EnumSet.of(LEAVE_PLATFORM, INTERCHANGE_DEPART,
            DEPART, WALKS_TO_STATION, DIVERSION_DEPART);

    private final T relationship;

    protected GraphRelationshipProperties(T relationship) {
        this.relationship = relationship;
    }

    protected abstract void invalidateCache();

    public Object getPropertyForTesting(final GraphPropertyKey graphPropertyKey) {
        return relationship.getProperty(graphPropertyKey.getText());
    }

    @Override
    public void setTransportMode(final TransportMode transportMode) {
        relationship.setTransportMode(transportMode);
        invalidateCache();
    }

    @Override
    public void setTime(final TramTime tramTime) {
        relationship.setTime(tramTime);
        invalidateCache();
    }

    @Override
    public void setHour(final int hour) {
        relationship.setProperty(HOUR.getText(), hour);
        invalidateCache();
    }

    @Override
    public void setCost(final Duration cost) {
        relationship.setCost(cost);
        invalidateCache();
    }

    @Override
    public <C extends GraphProperty & CoreDomain & HasId<C>> void set(final C domainItem) {
        set(domainItem, relationship);
        invalidateCache();
    }

    @Override
    public boolean hasProperty(final GraphPropertyKey propertyKey) {
        return relationship.hasProperty(propertyKey.getText());
    }

    @Override
    public void setRouteStationId(final IdFor<RouteStation> routeStationId) {
        relationship.setProperty(ROUTE_STATION_ID.getText(), routeStationId.getGraphId());
        invalidateCache();
    }

    @Override
    public void setStopSeqNum(final int sequenceNumber) {
        relationship.setProperty(STOP_SEQ_NUM.getText(), sequenceNumber);
        invalidateCache();
    }

    @Override
    public void setDateTimeRange(final DateTimeRange dateTimeRange) {
        final DateRange dateRange = dateTimeRange.getDateRange();
        final TimeRange timeRange = dateTimeRange.getTimeRange();

        setDateRange(dateRange);
        setTimeRange(timeRange);
    }

    @JsonIgnore
    @Override
    public int getStopSeqNumber() {
        return (int) relationship.getProperty(STOP_SEQ_NUM.getText());
    }

    @Override
    public void setDateRange(final DateRange range) {
        setStartDate(range.getStartDate().toLocalDate());
        setEndDate(range.getEndDate().toLocalDate());
    }

    @Override
    public void setTimeRange(final TimeRange timeRange) {
        if (timeRange.allDay()) {
            relationship.setProperty(ALL_DAY.getText(), "");
            relationship.removeProperty(START_TIME.getText());
            relationship.removeProperty(END_TIME.getText());
        } else {
            setStartTime(timeRange.getStart());
            setEndTime(timeRange.getEnd());
            relationship.removeProperty(ALL_DAY.getText());
        }
        invalidateCache();
    }

    @Override
    public void setStartTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for start time next");
        }
        relationship.setProperty(START_TIME.getText(), tramTime.asLocalTime());
        invalidateCache();
    }

    @Override
    public void setEndTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for end time next");
        }
        relationship.setProperty(END_TIME.getText(), tramTime.asLocalTime());
        invalidateCache();
    }

    @Override
    public void setEndDate(final LocalDate localDate) {
        relationship.setProperty(END_DATE.getText(), localDate);
        invalidateCache();
    }

    @Override
    public void setStartDate(final LocalDate localDate) {
        relationship.setProperty(START_DATE.getText(), localDate);
        invalidateCache();
    }

    // TODO Push specifics into implementations of GraphEntityProperties.GraphProps
    @Override
    public void addTransportMode(final TransportMode mode) {
        invalidateCache();
        relationship.addTransportMode(mode);
    }

    @Override
    public void addTripId(final IdFor<Trip> tripId) {
        invalidateCache();
        relationship.addTripId(tripId);
    }

    @JsonIgnore
    @Override
    public Duration getCost() {
        final TransportRelationshipTypes relationshipType = getType();
        if (TransportRelationshipTypes.hasCost(relationshipType)) {
            return relationship.getCost();
        } else {
            return Duration.ZERO;
        }
    }

    @JsonIgnore
    @Override
    public DateRange getDateRange() {
        final LocalDate start = getStartDate();
        final LocalDate end = getEndDate();
        return new DateRange(TramDate.of(start), TramDate.of(end));
    }

    @JsonIgnore
    @Override
    public int getHour() {
        return (int) relationship.getProperty(HOUR.getText());
    }

    @JsonIgnore
    @Override
    public TramTime getTime() {
        return relationship.getTime();
    }

    @JsonIgnore
    @Override
    public TimeRange getTimeRange() {
        if (relationship.hasProperty(ALL_DAY.getText())) {
            return TimeRange.AllDay();
        } else {
            final TramTime start = getStartTime();
            final TramTime end = getEndTime();
            return TimeRange.of(start, end);
        }
    }

    @JsonIgnore
    @Override
    public DateTimeRange getDateTimeRange() {
        final DateRange dateRange = getDateRange();
        final TimeRange timeRange = getTimeRange();
        return DateTimeRange.of(dateRange, timeRange);
    }

    @JsonIgnore
    public IdSet<Trip> getTripIds() {
        return relationship.getTripIds();
    }

    /***
     * Note: Assumes only called for relationships having TRIP_ID_LIST property, i.e. SERVICE_TO relationship type
     * @param tripId The id for a trip
     * @return true if trip id is contained in the list
     */
    @Override
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        return relationship.hasTripIdInList(tripId);
    }

    @JsonIgnore
    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return relationship.getTransportModes();
    }

    @JsonIgnore
    public IdFor<Route> getRouteId() {
        return getId(Route.class);
    }

    @JsonIgnore
    public IdFor<Service> getServiceId() {
        return getId(Service.class);
    }

    @JsonIgnore
    public IdFor<Trip> getTripId() {
        return getId(Trip.class);
    }

    @JsonIgnore
    public IdFor<Station> getStationId() {
        return getId(Station.class);
    }

    <K extends CoreDomain> IdFor<K> getId(Class<K> theClass) {
        return getIdFor(theClass, relationship);
    }

    @JsonIgnore
    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(relationship);
    }

    @JsonIgnore
    public Map<String,Object> getAllProperties() {
        return getAllProperties(relationship);
    }

    @JsonIgnore
    public boolean isDayOffset() {
        // todo should this be checking if set instead?
        return (Boolean) relationship.getProperty(DAY_OFFSET.getText());
    }

    public boolean validOn(final TramDate tramDate) {
        final LocalDate localDate = tramDate.toLocalDate();
        final LocalDate startDate = getStartDate();
        if (localDate.isBefore(startDate)) {
            return false;
        }
        final LocalDate endDate = getEndDate();
        return !localDate.isAfter(endDate);
    }

    @JsonIgnore
    private LocalDate getEndDate() {
        return (LocalDate) relationship.getProperty(END_DATE.getText());
    }

    @JsonIgnore
    private LocalDate getStartDate() {
        return (LocalDate) relationship.getProperty(START_DATE.getText());
    }

    @JsonIgnore
    @Override
    public TramTime getStartTime() {
        final LocalTime localTime = (LocalTime) relationship.getProperty(START_TIME.getText());
        return TramTime.ofHourMins(localTime);
    }

    @JsonIgnore
    @Override
    public TramTime getEndTime() {
        final LocalTime localTime = (LocalTime) relationship.getProperty(END_TIME.getText());
        return TramTime.ofHourMins(localTime);
    }

    @JsonIgnore
    @Override
    public LocationId<?> getLocationId(GraphTransaction txn) {
        final TransportRelationshipTypes transportRelationshipTypes = getType();
        if (HAS_STATION_ID.contains(transportRelationshipTypes)) {
            return LocationId.wrap(getStationId());
        } else if (transportRelationshipTypes==GROUPED_TO_PARENT) {
            return LocationId.wrap(getStationGroupId(txn));
        } else {
            throw new RuntimeException("Unsupported relationship type " + transportRelationshipTypes);
        }
    }

}
