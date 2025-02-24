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
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.internal.helpers.collection.Iterables;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class ImmutableGraphRelationship implements GraphRelationship {
    private final MutableGraphRelationship underlying;

    public ImmutableGraphRelationship(final MutableGraphRelationship underlying) {
        this.underlying = underlying;
    }

    public static ResourceIterable<Relationship> convertIterable(final Stream<ImmutableGraphRelationship> stream) {
        final Stream<Relationship> mapped = stream.map(ImmutableGraphRelationship::getRelationship);

        final Iterable<Relationship> iterable = new Iterable<>() {
            @NotNull
            @Override
            public Iterator<Relationship> iterator() {
                return mapped.iterator();
            }
        };
        return Iterables.asResourceIterable(iterable);
    }

    private Relationship getRelationship() {
        return underlying.getRelationship();
    }

    @Override
    public GraphRelationshipId getId() {
        return underlying.getId();
    }

    @Override
    public TramTime getTime() {
        return underlying.getTime();
    }

    @Override
    public int getHour() {
        return underlying.getHour();
    }

    @Override
    public Duration getCost() {
        return underlying.getCost();
    }

    @Override
    public GraphNode getEndNode(GraphTransaction txn) {
        return underlying.getEndNode(txn);
    }

    @Override
    public GraphNode getStartNode(GraphTransaction txn) {
        return underlying.getStartNode(txn);
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return underlying.getTransportModes();
    }

    @Override
    public TransportRelationshipTypes getType() {
        return underlying.getType();
    }

    @Override
    public IdFor<Route> getRouteId() {
        return underlying.getRouteId();
    }

    @Override
    public IdFor<Service> getServiceId() {
        return underlying.getServiceId();
    }

    @Override
    public IdFor<Trip> getTripId() {
        return underlying.getTripId();
    }

    @Override
    public boolean isType(TransportRelationshipTypes transportRelationshipType) {
        return underlying.isType(transportRelationshipType);
    }

    @Override
    public IdFor<RouteStation> getRouteStationId() {
        return underlying.getRouteStationId();
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return underlying.getAllProperties();
    }

    @Override
    public boolean isDayOffset() {
        return underlying.isDayOffset();
    }

    @Override
    public boolean validOn(final TramDate tramDate) {
        return underlying.validOn(tramDate);
    }

    @Override
    public IdFor<Station> getStationId() {
        return underlying.getStationId();
    }

    @Override
    public GraphNodeId getEndNodeId(final GraphTransaction txn) {
        return underlying.getEndNodeId(txn);
    }

    @Override
    public boolean hasProperty(GraphPropertyKey graphPropertyKey) {
        return underlying.hasProperty(graphPropertyKey);
    }

    @Override
    public int getStopSeqNumber() {
        return underlying.getStopSeqNumber();
    }

    @Override
    public IdFor<Station> getEndStationId() {
        return underlying.getEndStationId();
    }

    @Override
    public IdFor<Station> getStartStationId() {
        return underlying.getStartStationId();
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return underlying.getStationGroupId();
    }

    @Override
    public IdSet<Trip> getTripIds() {
        return underlying.getTripIds();
    }

    @Override
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        // caching here seemed to have minimal impact, likely not seeing repeat calls for the same trip ID
//        return tripIdPresent.getOrPopulate(tripId, unused -> underlying.hasTripIdInList(tripId));
        return underlying.hasTripIdInList(tripId);
    }

    @Override
    public String toString() {
        return "ImmutableGraphRelationship{" +
                underlying.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutableGraphRelationship that = (ImmutableGraphRelationship) o;
        return Objects.equals(underlying, that.underlying);
    }

    @Override
    public int hashCode() {
        return Objects.hash(underlying);
    }

    @Override
    public DateRange getDateRange() {
        return underlying.getDateRange();
    }

    @Override
    public TimeRange getTimeRange() {
        return underlying.getTimeRange();
    }

    @Override
    public DateTimeRange getDateTimeRange() {
        return underlying.getDateTimeRange();
    }

    @Override
    public TramTime getStartTime() {
        return underlying.getStartTime();
    }

    @Override
    public TramTime getEndTime() {
        return underlying.getEndTime();
    }

    @Override
    public GraphNodeId getStartNodeId(GraphTransaction txn) {
        return underlying.getStartNodeId(txn);
    }
}
