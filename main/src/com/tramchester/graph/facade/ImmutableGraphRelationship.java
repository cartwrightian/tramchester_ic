package com.tramchester.graph.facade;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
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
import java.util.stream.Stream;

public class ImmutableGraphRelationship implements GraphRelationship {
    private final MutableGraphRelationship underlying;

    public ImmutableGraphRelationship(MutableGraphRelationship underlying) {
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
    public GraphNodeId getEndNodeId(GraphTransaction txn) {
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
    public IdFor<StationGroup> getStationGroupId() {
        return underlying.getStationGroupId();
    }
}
