package com.tramchester.graph.facade;

import com.google.common.collect.Streams;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public GraphRelationship createRelationshipTo(GraphTransaction txn, MutableGraphNode end, TransportRelationshipTypes relationshipTypes) {
        Relationship relationshipTo = node.createRelationshipTo(end.node, relationshipTypes);
        return txn.wrapRelationship(relationshipTo);
    }

    public void delete() {
        node.delete();
    }

    public Stream<GraphRelationship> getRelationships(GraphTransaction txn, Direction direction, TransportRelationshipTypes relationshipType) {
        return node.getRelationships(direction, relationshipType).stream().map(txn::wrapRelationship);
    }

    public Stream<GraphRelationship> getRelationships(GraphTransaction txn, Direction direction, TransportRelationshipTypes... transportRelationshipTypes) {
        return node.getRelationships(direction, transportRelationshipTypes).stream().map(txn::wrapRelationship);
    }

    @Override
    public TransportMode getTransportMode() {
        short number = (short) node.getProperty(TRANSPORT_MODE.getText());
        return TransportMode.fromNumber(number);
    }

    @Override
    public Integer getHour() {
        return (int) node.getProperty(HOUR.getText());
    }



    @Override
    public String toString() {
        return "GraphNode{" +
                "neo4jNode=" + node +
                '}';
    }

    public boolean hasRelationship(Direction direction, TransportRelationshipTypes transportRelationshipTypes) {
        return node.hasRelationship(direction, transportRelationshipTypes);
    }

    public void addLabel(Label label) {
        node.addLabel(label);
    }

    public void setHourProp(Integer hour) {
        node.setProperty(HOUR.getText(), hour);
    }

    public boolean hasLabel(GraphLabel graphLabel) {
        return node.hasLabel(graphLabel);
    }

    public Object getProperty(String key) {
        return node.getProperty(key);
    }

    public Relationship getSingleRelationship(TransportRelationshipTypes transportRelationshipTypes, Direction direction) {
        return node.getSingleRelationship(transportRelationshipTypes,direction);
    }

    public EnumSet<GraphLabel> getLabels() {
        final Iterable<Label> iter = node.getLabels();
        final Set<GraphLabel> set = Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toSet());
        return EnumSet.copyOf(set);
    }

    public void setProperty(GraphPropertyKey graphPropertyKey, String name) {
        node.setProperty(graphPropertyKey.getText(), name);
    }

    public void setTime(TramTime tramTime) {
        setTime(tramTime, node);
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
    public IdFor<NaptanArea> getAreaIdFromGrouped() {
        //        return getIdFromGraphEntity(entity, AREA_ID, NaptanArea.class);
        return getIdFor(NaptanArea.class, node);
    }

    public IdFor<Trip> getTripId() {
        return getIdFor(Trip.class, node);
    }

    public TramTime getTime() {
        return getTime(node);
    }

    public LatLong getLatLong() {
        final double lat = (double) getProperty(LATITUDE.getText());
        final double lon = (double) getProperty(LONGITUDE.getText());
        return new LatLong(lat, lon);
    }

    public boolean hasTripId() {
        return node.hasProperty(TRIP_ID.getText());
    }

    public PlatformId getPlatformId() {
        IdFor<Station> stationId = getStationId(); // GraphProps.getStationIdFrom(node);
        String platformNumber = node.getProperty(PLATFORM_NUMBER.getText()).toString();
        return PlatformId.createId(stationId, platformNumber);
    }

    public boolean hasStationId() {
        return node.hasProperty(STATION_ID.getText());
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

    public void set(Station station) {
        super.set(station, node);
    }

    public void set(Platform platform) {
        super.set(platform, node);
    }

    public void set(Route route) {
        super.set(route, node);
    }

    public void set(Service service) {
        super.set(service, node);
    }

    public void set(StationGroup stationGroup) {
        super.set(stationGroup, node);
    }

    public void set(RouteStation routeStation) {
        super.set(routeStation, node);
    }

    public void setTransportMode(TransportMode first) {
        node.setProperty(TRANSPORT_MODE.getText(), first.getNumber());
    }
}
