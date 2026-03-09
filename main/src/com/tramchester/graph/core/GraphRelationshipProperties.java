package com.tramchester.graph.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tramchester.graph.GraphPropertyKey.*;
import static com.tramchester.graph.reference.TransportRelationshipTypes.*;

public abstract class GraphRelationshipProperties <T extends GraphEntityProperties.GraphProps<T>>
        extends GraphEntityProperties<T> implements MutableGraphRelationship {

    private static final EnumSet<TransportRelationshipTypes> HAS_STATION_ID = EnumSet.of(LEAVE_PLATFORM, INTERCHANGE_DEPART,
            DEPART, WALKS_TO_STATION, DIVERSION_DEPART);

    private final T relationshipProperties;

    protected GraphRelationshipProperties(T relationshipProperties) {
        this.relationshipProperties = relationshipProperties;
    }

    protected T copyProperties() {
        return relationshipProperties.copy();
    }

    protected abstract void invalidateCache();

    public Object getPropertyForTesting(final GraphPropertyKey graphPropertyKey) {
        return relationshipProperties.getProperty(graphPropertyKey);
    }

    @Override
    public void setTransportMode(final TransportMode transportMode) {
        relationshipProperties.setTransportMode(transportMode);
        invalidateCache();
    }

    @Override
    public void setTime(final TramTime tramTime) {
        relationshipProperties.setTime(tramTime);
        invalidateCache();
    }

    @Override
    public void setHour(final int hour) {
        relationshipProperties.setProperty(HOUR, hour);
        invalidateCache();
    }

    @Override
    public void setCost(final TramDuration cost) {
        relationshipProperties.setCost(cost);
        invalidateCache();
    }

    @Override
    public <C extends GraphProperty & CoreDomain & HasId<C>> void set(final C domainItem) {
        set(domainItem, relationshipProperties);
        invalidateCache();
    }

    @Override
    public boolean hasProperty(final GraphPropertyKey propertyKey) {
        return relationshipProperties.hasProperty(propertyKey);
    }

    @Override
    public void setRouteStationId(final IdFor<RouteStation> routeStationId) {
        relationshipProperties.setProperty(ROUTE_STATION_ID, routeStationId.getGraphId());
        invalidateCache();
    }

    @Override
    public void setStopSeqNum(final int sequenceNumber) {
        relationshipProperties.setProperty(STOP_SEQ_NUM, sequenceNumber);
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
        return (int) relationshipProperties.getProperty(STOP_SEQ_NUM);
    }

    @Override
    public void setDateRange(final DateRange range) {
        setStartDate(range.getStartDate());
        setEndDate(range.getEndDate());
    }

    @Override
    public void setTimeRange(final TimeRange timeRange) {
        // TODO Into time range object
        if (timeRange.allDay()) {
            relationshipProperties.setProperty(ALL_DAY, "");
            relationshipProperties.removeProperty(START_TIME);
            relationshipProperties.removeProperty(END_TIME);
        } else {
            setStartTime(timeRange.getStart());
            setEndTime(timeRange.getEnd());
            relationshipProperties.removeProperty(ALL_DAY);
        }
        invalidateCache();
    }

    @Override
    public void setStartTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for start time next");
        }
        relationshipProperties.setProperty(START_TIME, tramTime);
        invalidateCache();
    }

    @Override
    public void setEndTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for end time next");
        }
        relationshipProperties.setProperty(END_TIME, tramTime);
        invalidateCache();
    }

    @Override
    public void setEndDate(final TramDate tramDate) {
        relationshipProperties.setProperty(END_DATE, tramDate);
        invalidateCache();
    }

    @Override
    public void setStartDate(final TramDate localDate) {
        relationshipProperties.setProperty(START_DATE, localDate);
        invalidateCache();
    }

    // TODO Push specifics into implementations of GraphEntityProperties.GraphProps
    @Override
    public void addTransportMode(final TransportMode mode) {
        invalidateCache();
        relationshipProperties.addTransportMode(mode);
    }

    @Override
    public void addTripId(final IdFor<Trip> tripId) {
        invalidateCache();
        relationshipProperties.addTripId(tripId);
    }

    @JsonIgnore
    @Override
    public TramDuration getCost() {
        final TransportRelationshipTypes relationshipType = getType();
        if (TransportRelationshipTypes.hasCost(relationshipType)) {
            return relationshipProperties.getCost();
        } else {
            return TramDuration.ZERO;
        }
    }

    @JsonIgnore
    @Override
    public DateRange getDateRange() {
        final TramDate start = getStartDate();
        final TramDate end = getEndDate();
        return new DateRange(start, end);
    }

    @JsonIgnore
    @Override
    public int getHour() {
        return (int) relationshipProperties.getProperty(HOUR);
    }

    @JsonIgnore
    @Override
    public TramTime getTime() {
        return relationshipProperties.getTime();
    }

    @JsonIgnore
    @Override
    public TimeRange getTimeRange() {
        // TODO Into time range object
        if (relationshipProperties.hasProperty(ALL_DAY)) {
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
    public ImmutableIdSet<Trip> getTripIds() {
        return relationshipProperties.getTripIds();
    }

    /***
     * Note: Assumes only called for relationships having TRIP_ID_LIST property, i.e. SERVICE_TO relationship type
     * @param tripId The id for a trip
     * @return true if trip id is contained in the list
     */
    @Override
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        return relationshipProperties.hasTripIdInList(tripId);
    }

    @JsonIgnore
    @Override
    public ImmutableEnumSet<TransportMode> getTransportModes() {
        return relationshipProperties.getTransportModes();
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
        return getIdFor(theClass, relationshipProperties);
    }

    @JsonIgnore
    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(relationshipProperties);
    }

    @JsonIgnore
    @Override
    public Map<GraphPropertyKey, Object> getAllProperties() {
        return getAllProperties(relationshipProperties);
    }

//    @JsonIgnore
//    public boolean isDayOffset() {
//        // todo should this be checking if set instead?
//        return (Boolean) relationship.getProperty(DAY_OFFSET.getText());
//    }

    public boolean validOn(final TramDate tramDate) {
        final TramDate startDate = getStartDate();
        if (tramDate.isBefore(startDate)) {
            return false;
        }
        final TramDate endDate = getEndDate();
        return !tramDate.isAfter(endDate);
    }

    @JsonIgnore
    private TramDate getEndDate() {
        return (TramDate) relationshipProperties.getProperty(END_DATE);
    }

    @JsonIgnore
    private TramDate getStartDate() {
        return (TramDate) relationshipProperties.getProperty(START_DATE);
    }

    @JsonIgnore
    @Override
    public TramTime getStartTime() {
        return (TramTime) relationshipProperties.getProperty(START_TIME);
    }

    @JsonIgnore
    @Override
    public TramTime getEndTime() {
        return (TramTime) relationshipProperties.getProperty(END_TIME);
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

    @JsonIgnore
    public Set<GraphPropertyKey> getUnusedProps() {
        return relationshipProperties.getUnused();
    }
}
