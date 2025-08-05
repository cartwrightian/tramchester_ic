package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class GraphNodeInMemory implements MutableGraphNode {

    private final GraphNodeId id;
    private final EnumSet<GraphLabel> labels;

    public GraphNodeInMemory(final GraphNodeId id, final EnumSet<GraphLabel> labels) {
        this.id = id;
        this.labels = labels;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphNodeInMemory that = (GraphNodeInMemory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "GraphNodeInMemory{" +
                "id=" + id +
                ", labels=" + labels +
                '}';
    }

    @Override
    public GraphNodeId getId() {
        return id;
    }

    @Override
    public IdFor<RouteStation> getRouteStationId() {
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
    public IdFor<Platform> getPlatformId() {
        return null;
    }

    @Override
    public IdFor<Station> getStationId() {
        return null;
    }

    @Override
    public IdFor<Station> getTowardsStationId() {
        return null;
    }

    @Override
    public IdFor<Route> getRouteId() {
        return null;
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return null;
    }

    @Override
    public IdFor<NPTGLocality> getAreaId() {
        return null;
    }

    @Override
    public BoundingBox getBounds() {
        return null;
    }

    @Override
    public boolean hasTripId() {
        return false;
    }

    @Override
    public boolean hasStationId() {
        return false;
    }

    @Override
    public TramTime getTime() {
        return null;
    }

    @Override
    public LatLong getLatLong() {
        return null;
    }

    @Override
    public TransportMode getTransportMode() {
        return null;
    }

    @Override
    public int getHour() {
        return 0;
    }

    @Override
    public boolean hasLabel(GraphLabel graphLabel) {
        return labels.contains(graphLabel);
    }

    @Override
    public EnumSet<GraphLabel> getLabels() {
        return labels;
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return Map.of();
    }

    @Override
    public boolean hasRelationship(GraphDirection direction, TransportRelationshipTypes transportRelationshipTypes) {
        return false;
    }

    @Override
    public ImmutableGraphRelationship getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, GraphDirection direction) {
        return null;
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes relationshipType) {
        return Stream.empty();
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes... transportRelationshipTypes) {
        return Stream.empty();
    }

    @Override
    public boolean hasOutgoingServiceMatching(GraphTransaction txn, IdFor<Trip> tripId) {
        return false;
    }

    @Override
    public Stream<ImmutableGraphRelationship> getOutgoingServiceMatching(GraphTransaction txn, IdFor<Trip> tripId) {
        return Stream.empty();
    }

    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isRelationship() {
        return false;
    }

    @Override
    public void delete() {

    }

    @Override
    public MutableGraphRelationship createRelationshipTo(MutableGraphTransaction txn,
                                                         MutableGraphNode end, TransportRelationshipTypes relationshipType) {
        return txn.createRelationship(this, end, relationshipType);
    }

    @Override
    public void addLabel(GraphLabel label) {

    }

    @Override
    public void setHourProp(Integer hour) {

    }

    @Override
    public void setTime(TramTime tramTime) {

    }

    @Override
    public void set(Station station) {

    }

    @Override
    public void set(Platform platform) {

    }

    @Override
    public void set(Route route) {

    }

    @Override
    public void set(Service service) {

    }

    @Override
    public void set(StationLocalityGroup stationGroup) {

    }

    @Override
    public void set(RouteStation routeStation) {

    }

    @Override
    public void setTransportMode(TransportMode first) {

    }

    @Override
    public void set(DataSourceInfo nameAndVersion) {

    }

    @Override
    public void setLatLong(LatLong latLong) {

    }

    @Override
    public void setBounds(BoundingBox bounds) {

    }

    @Override
    public void setWalkId(LatLong origin, UUID uid) {

    }

    @Override
    public void setPlatformNumber(Platform platform) {

    }

    @Override
    public void setSourceName(String sourceName) {

    }

    @Override
    public void setAreaId(IdFor<NPTGLocality> localityId) {

    }

    @Override
    public void setTowards(IdFor<Station> stationId) {

    }

    @Override
    public void set(Trip trip) {

    }

    @Override
    public Stream<MutableGraphRelationship> getRelationshipsMutable(MutableGraphTransaction txn, GraphDirection direction, TransportRelationshipTypes relationshipType) {
        return Stream.empty();
    }

    @Override
    public MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction tx, TransportRelationshipTypes transportRelationshipTypes, GraphDirection graphDirection) {
        return null;
    }
}
