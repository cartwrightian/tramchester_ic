package com.tramchester.graph.core.inMemory;

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
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;

public class GraphRelationshipInMemory implements MutableGraphRelationship {
    private final TransportRelationshipTypes relationshipType;
    private final GraphRelationshipId id;
    private final MutableGraphNode start;
    private final MutableGraphNode end;

    public GraphRelationshipInMemory(TransportRelationshipTypes relationshipType, GraphRelationshipId id,
                                     MutableGraphNode start, MutableGraphNode end) {
        this.relationshipType = relationshipType;
        this.id = id;
        this.start = start;
        this.end = end;
    }

    @Override
    public void delete() {

    }

    @Override
    public void setTransportMode(TransportMode transportMode) {

    }

    @Override
    public void setTime(TramTime tramTime) {

    }

    @Override
    public void setHour(int hour) {

    }

    @Override
    public void setCost(Duration cost) {

    }

    @Override
    public <C extends GraphProperty & CoreDomain & HasId<C>> void set(C domainItem) {

    }

    @Override
    public void setRouteStationId(IdFor<RouteStation> routeStationId) {

    }

    @Override
    public void setStopSeqNum(int sequenceNumber) {

    }

    @Override
    public void setDateTimeRange(DateTimeRange dateTimeRange) {

    }

    @Override
    public void setDateRange(DateRange range) {

    }

    @Override
    public void setTimeRange(TimeRange timeRange) {

    }

    @Override
    public void setStartTime(TramTime tramTime) {

    }

    @Override
    public void setEndTime(TramTime tramTime) {

    }

    @Override
    public void setEndDate(LocalDate localDate) {

    }

    @Override
    public void setStartDate(LocalDate localDate) {

    }

    @Override
    public void addTransportMode(TransportMode mode) {

    }

    @Override
    public void addTripId(IdFor<Trip> tripId) {

    }

    @Override
    public GraphRelationshipId getId() {
        return id;
    }

    @Override
    public TramTime getTime() {
        return null;
    }

    @Override
    public int getHour() {
        return 0;
    }

    @Override
    public Duration getCost() {
        return null;
    }

    @Override
    public GraphNode getEndNode(GraphTransaction txn) {
        return end;
    }

    @Override
    public GraphNode getStartNode(GraphTransaction txn) {
        return start;
    }

    @Override
    public GraphNodeId getStartNodeId(GraphTransaction txn) {
        return start.getId();
    }

    @Override
    public GraphNodeId getEndNodeId(GraphTransaction txn) {
        return end.getId();
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return null;
    }

    @Override
    public TransportRelationshipTypes getType() {
        return relationshipType;
    }

    @Override
    public IdFor<Route> getRouteId() {
        return null;
    }

    @Override
    public IdFor<Service> getServiceId() {
        return null;
    }

    @Override
    public IdFor<Trip> getTripId() {
        return null;
    }

    @Override
    public boolean isType(TransportRelationshipTypes transportRelationshipType) {
        return false;
    }

    @Override
    public IdFor<RouteStation> getRouteStationId() {
        return null;
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return Map.of();
    }

    @Override
    public boolean isDayOffset() {
        return false;
    }

    @Override
    public boolean validOn(TramDate tramDate) {
        return false;
    }

    @Override
    public IdFor<Station> getStationId() {
        return null;
    }

    @Override
    public boolean hasProperty(GraphPropertyKey graphPropertyKey) {
        return false;
    }

    @Override
    public int getStopSeqNumber() {
        return 0;
    }

    @Override
    public IdFor<Station> getEndStationId() {
        return null;
    }

    @Override
    public IdFor<Station> getStartStationId() {
        return null;
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return null;
    }

    @Override
    public IdSet<Trip> getTripIds() {
        return null;
    }

    @Override
    public DateRange getDateRange() {
        return null;
    }

    @Override
    public TimeRange getTimeRange() {
        return null;
    }

    @Override
    public DateTimeRange getDateTimeRange() {
        return null;
    }

    @Override
    public TramTime getStartTime() {
        return null;
    }

    @Override
    public TramTime getEndTime() {
        return null;
    }

    @Override
    public LocationId<?> getLocationId() {
        return null;
    }

    @Override
    public boolean hasTripIdInList(IdFor<Trip> tripId) {
        return false;
    }

    @Override
    public boolean isNode() {
        return false;
    }

    @Override
    public boolean isRelationship() {
        return true;
    }
}
