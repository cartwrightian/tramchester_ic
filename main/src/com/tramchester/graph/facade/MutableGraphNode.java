package com.tramchester.graph.facade;

import com.google.common.collect.Streams;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.HaveGraphProperties;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.*;

public class MutableGraphNode extends HaveGraphProperties implements GraphNode {
    private final Node node;
    private final GraphNodeId graphNodeId;

    MutableGraphNode(Node node, GraphNodeId graphNodeId) {
        if (node == null) {
            throw new RuntimeException("Null node passed");
        }
        this.node = node;
        this.graphNodeId = graphNodeId;
    }

    public GraphNodeId getId() {
        return graphNodeId;
    }

    public Node getNode() {
        return node;
    }

    public void delete() {
        node.delete();
    }

    ///// MUTATE ////////////////////////////////////////////////////////////

    public GraphRelationship createRelationshipTo(GraphTransaction txn, MutableGraphNode end, TransportRelationshipTypes relationshipTypes) {
        Relationship relationshipTo = node.createRelationshipTo(end.node, relationshipTypes);
        return txn.wrapRelationship(relationshipTo);
    }

    public void addLabel(Label label) {
        node.addLabel(label);
    }

    public void setHourProp(Integer hour) {
        node.setProperty(HOUR.getText(), hour);
    }

    public void setTime(TramTime tramTime) {
        setTime(tramTime, node);
    }

    public void set(Station station) {
        set(station, node);
    }

    public void set(Platform platform) {
        set(platform, node);
    }

    public void set(Route route) {
        set(route, node);
    }

    public void set(Service service) {
        set(service, node);
    }

    public void set(StationGroup stationGroup) {
        set(stationGroup, node);
    }

    public void set(RouteStation routeStation) {
        set(routeStation, node);
    }

    public void setTransportMode(TransportMode first) {
        node.setProperty(TRANSPORT_MODE.getText(), first.getNumber());
    }

    public void set(DataSourceInfo nameAndVersion) {
        DataSourceID sourceID = nameAndVersion.getID();
        node.setProperty(sourceID.name(), nameAndVersion.getVersion());
    }

    public void setLatLong(LatLong latLong) {
        node.setProperty(LATITUDE.getText(), latLong.getLat());
        node.setProperty(LONGITUDE.getText(), latLong.getLon());
    }

    public void setWalkId(LatLong origin, UUID uid) {
        node.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
    }

    public void setPlatformNumber(Platform platform) {
        node.setProperty(PLATFORM_NUMBER.getText(), platform.getPlatformNumber());
    }

    public void setSourceName(String sourceName) {
        node.setProperty(SOURCE_NAME_PROP.getText(), sourceName);
    }

    public void setAreaId(IdFor<NaptanArea> areaId) {
        node.setProperty(AREA_ID.getText(), areaId.getGraphId());
    }

    public void setTowards(IdFor<Station> stationId) {
        node.setProperty(TOWARDS_STATION_ID.getText(), stationId.getGraphId());
    }

    ///// GET //////////////////////////////////////////////////

    public EnumSet<GraphLabel> getLabels() {
        final Iterable<Label> iter = node.getLabels();
        final Set<GraphLabel> set = Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toSet());
        return EnumSet.copyOf(set);
    }

    public Stream<GraphRelationship> getRelationships(GraphTransaction txn, Direction direction, TransportRelationshipTypes relationshipType) {
        return node.getRelationships(direction, relationshipType).stream().map(txn::wrapRelationship);
    }

    public Stream<GraphRelationship> getRelationships(GraphTransaction txn, Direction direction, TransportRelationshipTypes... transportRelationshipTypes) {
        return node.getRelationships(direction, transportRelationshipTypes).stream().map(txn::wrapRelationship);
    }

    @Override
    public TransportMode getTransportMode() {
        short number = (short) super.getProperty(TRANSPORT_MODE, node);
        return TransportMode.fromNumber(number);
    }

    @Override
    public Integer getHour() {
        return (int) super.getProperty(HOUR, node);
    }

    public boolean hasRelationship(Direction direction, TransportRelationshipTypes transportRelationshipTypes) {
        return node.hasRelationship(direction, transportRelationshipTypes);
    }

    public boolean hasLabel(GraphLabel graphLabel) {
        return node.hasLabel(graphLabel);
    }

    @Override
    public GraphRelationship getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, Direction direction) {
        Relationship found = node.getSingleRelationship(transportRelationshipTypes, direction);
        if (found==null) {
            return null;
        }
        return txn.wrapRelationship(found);
    }

    public IdFor<Station> getStationId() {
        return getIdFor(Station.class, node);
    }

    public void set(Trip trip) {
        set(trip, node);
    }

    public Map<String,Object> getAllProperties() {
        return getAllProperties(node);
    }

    @Override
    public Traverser getTraverserFor(TraversalDescription traversalDesc) {
        return traversalDesc.traverse(node);
    }

    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(node);
    }

    public IdFor<Service> getServiceId() {
        return getIdFor(Service.class, node);
    }

    @Override
    public IdFor<Route> getRouteId() {
        return getIdFor(Route.class, node);
    }

    @Override
    public IdFor<NaptanArea> getAreaId() {
        return getIdFor(NaptanArea.class, node);
    }

    public IdFor<Trip> getTripId() {
        return getIdFor(Trip.class, node);
    }

    public TramTime getTime() {
        return getTime(node);
    }

    public LatLong getLatLong() {
        final double lat = (double) super.getProperty(LATITUDE, node);
        final double lon = (double) super.getProperty(LONGITUDE, node);
        return new LatLong(lat, lon);
    }

    public boolean hasTripId() {
        return node.hasProperty(TRIP_ID.getText());
    }

    public PlatformId getPlatformId() {
        IdFor<Station> stationId = getStationId();
        String platformNumber =  node.getProperty(PLATFORM_NUMBER.getText()).toString();
        return PlatformId.createId(stationId, platformNumber);
    }

    public boolean hasStationId() {
        return super.hasProperty(STATION_ID, node);
    }

    ///// utility ////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "GraphNode{" +
                "neo4jNode=" + node +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableGraphNode graphNode = (MutableGraphNode) o;
        return Objects.equals(graphNodeId, graphNode.graphNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphNodeId);
    }

}