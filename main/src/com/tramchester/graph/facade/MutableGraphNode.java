package com.tramchester.graph.facade;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.*;
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
import java.util.UUID;
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

    public MutableGraphRelationship createRelationshipTo(final MutableGraphTransaction txn, final MutableGraphNode end,
                                                         final TransportRelationshipTypes relationshipType) {
        final Relationship relationshipTo = node.createRelationshipTo(end.node, relationshipType);
        return txn.wrapRelationshipMutable(relationshipTo);
    }

    public void addLabel(final Label label) {
        node.addLabel(label);
    }

    public void setHourProp(final Integer hour) {
        node.setProperty(HOUR.getText(), hour);
    }

    public void setTime(final TramTime tramTime) {
        setTime(tramTime, node);
    }

    public void set(final Station station) {
        set(station, node);
    }

    public void set(final Platform platform) {
        set(platform, node);
    }

    public void set(final Route route) {
        set(route, node);
    }

    public void set(final Service service) {
        set(service, node);
    }

    public void set(final StationGroup stationGroup) {
        set(stationGroup, node);
    }

    public void set(final RouteStation routeStation) {
        set(routeStation, node);
    }

    public void setTransportMode(final TransportMode first) {
        node.setProperty(TRANSPORT_MODE.getText(), first.getNumber());
    }

    public void set(final DataSourceInfo nameAndVersion) {
        DataSourceID sourceID = nameAndVersion.getID();
        node.setProperty(sourceID.name(), nameAndVersion.getVersion());
    }

    public void setLatLong(final LatLong latLong) {
        node.setProperty(LATITUDE.getText(), latLong.getLat());
        node.setProperty(LONGITUDE.getText(), latLong.getLon());
    }

    public void setWalkId(final LatLong origin, final UUID uid) {
        node.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
    }

    public void setPlatformNumber(final Platform platform) {
        node.setProperty(PLATFORM_NUMBER.getText(), platform.getPlatformNumber());
    }

    public void setSourceName(final String sourceName) {
        node.setProperty(SOURCE_NAME_PROP.getText(), sourceName);
    }

    public void setAreaId(final IdFor<NPTGLocality> localityId) {
        node.setProperty(AREA_ID.getText(), localityId.getGraphId());
    }

    public void setTowards(final IdFor<Station> stationId) {
        node.setProperty(TOWARDS_STATION_ID.getText(), stationId.getGraphId());
    }

    ///// GET //////////////////////////////////////////////////

    public EnumSet<GraphLabel> getLabels() {
        return GraphLabel.from(node.getLabels());
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final Direction direction,
                                                               final TransportRelationshipTypes relationshipType) {
        return node.getRelationships(direction, relationshipType).stream().map(txn::wrapRelationship);
    }

    public Stream<MutableGraphRelationship> getRelationshipsMutable(final MutableGraphTransaction txn, final Direction direction,
                                                                    final TransportRelationshipTypes relationshipType) {
        return node.getRelationships(direction, relationshipType).stream().map(txn::wrapRelationshipMutable);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final Direction direction,
                                                               final TransportRelationshipTypes... transportRelationshipTypes) {
        return node.getRelationships(direction, transportRelationshipTypes).stream().
                map(txn::wrapRelationship);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getAllRelationships(final GraphTransaction txn, final Direction direction) {
        return node.getRelationships(direction).stream().map(txn::wrapRelationship);
    }

    @Override
    public TransportMode getTransportMode() {
        short number = (short) super.getProperty(TRANSPORT_MODE, node);
        return TransportMode.fromNumber(number);
    }

    @Override
    public Integer getHour() {
        return GraphLabel.getHourFrom(getLabels());
    }

    public boolean hasRelationship(final Direction direction, final TransportRelationshipTypes transportRelationshipTypes) {
        return node.hasRelationship(direction, transportRelationshipTypes);
    }

    @Override
    public boolean hasLabel(final GraphLabel graphLabel) {
        return node.hasLabel(graphLabel);
    }

    @Override
    public ImmutableGraphRelationship getSingleRelationship(MutableGraphTransaction txn, TransportRelationshipTypes transportRelationshipType, Direction direction) {
        final Relationship found = node.getSingleRelationship(transportRelationshipType, direction);
        if (found==null) {
            return null;
        }
        return txn.wrapRelationship(found);
    }

    public MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction txn, TransportRelationshipTypes transportRelationshipType, Direction direction) {
        final Relationship found = node.getSingleRelationship(transportRelationshipType, direction);
        if (found==null) {
            return null;
        }
        return txn.wrapRelationshipMutable(found);
    }

    <T extends CoreDomain> IdFor<T> getId(Class<T> theClass) {
        return getIdFor(theClass, node);
    }

    <DT extends CoreDomain> Boolean hasIdFor(Class<DT> theClass) {
        return node.hasProperty(GraphPropertyKey.getFor(theClass).getText());
    }

    public IdFor<Station> getStationId() {
        return getIdFor(Station.class, node);
    }

    public void set(final Trip trip) {
        set(trip, node);
    }

    public Map<String,Object> getAllProperties() {
        return getAllProperties(node);
    }

    @Override
    public Traverser getTraverserFor(final TraversalDescription traversalDesc) {
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
    public IdFor<StationGroup> getStationGroupId() {
        return getIdFor(StationGroup.class, node);
    }

    @Override
    public IdFor<NPTGLocality> getAreaId() {
        return getIdFor(NPTGLocality.class, node);
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
        return hasIdFor(Trip.class);
    }

    public PlatformId getPlatformId() {
        final IdFor<Station> stationId = getStationId();
        final String platformNumber =  node.getProperty(PLATFORM_NUMBER.getText()).toString();
        return PlatformId.createId(stationId, platformNumber);
    }

    public boolean hasStationId() {
        return hasIdFor(Station.class);
    }

    @Override
    public IdFor<Station> getTowardsStationId() {
        String text = (String) node.getProperty(TOWARDS_STATION_ID.getText());
        if (text==null) {
            return Station.InvalidId();
        }
        return Station.createId(text);
    }

    ///// utility ////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "MutableGraphNode{" +
                "node=" + node +
                ", graphNodeId=" + graphNodeId +
                "}";
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
