package com.tramchester.graph;

import com.google.common.collect.Streams;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.*;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphNode extends HaveGraphProperties {
    private final Node theNode;
    private final long id;

    public static GraphNode from(Node neo4jNode) {
        // preserve existing i/f for now
        if (neo4jNode==null) {
            return null;
        }
        return new GraphNode(neo4jNode);
    }

    GraphNode(Node theNode) {
        if (theNode==null) {
            throw new RuntimeException("Null node passed");
        }
        this.theNode = theNode;

        // todo remove/replace with get element Id
        this.id = theNode.getId();
    }

    public static GraphNode fromEnd(Path path) {
        return new GraphNode(path.endNode());
    }

    @Deprecated
    public Long getId() {
        return id;
    }

    public static GraphNode fromEnd(GraphRelationship relationship) {
        return relationship.getEndNode();
    }

    public static GraphNode fromStart(GraphRelationship relationship) {
        return relationship.getStartNode();
    }

    public Node getNode() {
        return theNode;
    }

    public GraphRelationship createRelationshipTo(GraphNode end, TransportRelationshipTypes relationshipTypes) {
        return new GraphRelationship(theNode.createRelationshipTo(end.theNode, relationshipTypes));
    }

    public void delete() {
        theNode.delete();
    }

    public Stream<GraphRelationship> getRelationships(Direction direction, TransportRelationshipTypes relationshipType) {
        return theNode.getRelationships(direction, relationshipType).stream().map(GraphRelationship::new);
    }

    public Stream<GraphRelationship> getRelationships(Direction direction, TransportRelationshipTypes... transportRelationshipTypes) {
        return theNode.getRelationships(direction, transportRelationshipTypes).stream().map(GraphRelationship::new);
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "neo4jNode=" + theNode +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return id == graphNode.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean hasRelationship(Direction direction, TransportRelationshipTypes transportRelationshipTypes) {
        return theNode.hasRelationship(direction, transportRelationshipTypes);
    }

    public void addLabel(Label label) {
        theNode.addLabel(label);
    }

    public void setHourProp(Integer hour) {
        theNode.setProperty(HOUR.getText(), hour);
    }


    public boolean hasLabel(GraphLabel graphLabel) {
        return theNode.hasLabel(graphLabel);
    }

    public Object getProperty(String key) {
        return theNode.getProperty(key);
    }

    public Relationship getSingleRelationship(TransportRelationshipTypes transportRelationshipTypes, Direction direction) {
        return theNode.getSingleRelationship(transportRelationshipTypes,direction);
    }

    public EnumSet<GraphLabel> getLabels() {
        final Iterable<Label> iter = theNode.getLabels();
        final Set<GraphLabel> set = Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toSet());
        return EnumSet.copyOf(set);
    }

    public void setProperty(GraphPropertyKey graphPropertyKey, String name) {
        theNode.setProperty(graphPropertyKey.getText(), name);
    }

    public void setTime(TramTime tramTime) {
        setTime(tramTime, theNode);
    }

    public IdFor<Station> getStationId() {
        return getIdFor(Station.class, theNode);
    }

    public void set(Trip trip) {
        set(trip, theNode);
    }

    public Map<String,Object> getAllProperties() {
        return getAllProperties(theNode);
    }

    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(theNode);
    }

    public IdFor<Service> getServiceId() {
        return getIdFor(Service.class, theNode);
    }

    public IdFor<Trip> getTripId() {
        return getIdFor(Trip.class, theNode);
    }

    public TramTime getTime() {
        return getTime(theNode);
    }

    public LatLong getLatLong() {
        final double lat = (double) getProperty(LATITUDE.getText());
        final double lon = (double) getProperty(LONGITUDE.getText());
        return new LatLong(lat, lon);
    }

    public boolean hasTripId() {
        return theNode.hasProperty(TRIP_ID.getText());
    }

    public PlatformId getPlatformId() {
        IdFor<Station> stationId = GraphProps.getStationIdFrom(theNode);
        String platformNumber = theNode.getProperty(PLATFORM_NUMBER.getText()).toString();
        return PlatformId.createId(stationId, platformNumber);
    }

    public boolean hasStationId() {
        return theNode.hasProperty(STATION_ID.getText());
    }
}
