package com.tramchester.graph.facade;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.HaveGraphProperties;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.SharedNodeCache;
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
import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;

public class MutableGraphNode extends HaveGraphProperties implements GraphNode {
    private final Node node;
    private final GraphNodeId graphNodeId;
    private final SharedNodeCache.InvalidatesCacheForNode invalidatesCacheForNode;

    MutableGraphNode(Node node, GraphNodeId graphNodeId, SharedNodeCache.InvalidatesCacheForNode invalidatesCacheForNode) {
        this.invalidatesCacheForNode = invalidatesCacheForNode;
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
        invalidateCache();
        node.delete();
    }

    private void invalidateCache() {
        invalidatesCacheForNode.remove();
    }

    ///// MUTATE ////////////////////////////////////////////////////////////

    public MutableGraphRelationship createRelationshipTo(final MutableGraphTransactionNeo4J txn, final MutableGraphNode end,
                                                         final TransportRelationshipTypes relationshipType) {
        final Relationship relationshipTo = node.createRelationshipTo(end.node, relationshipType);
        return txn.wrapRelationshipMutable(relationshipTo);
    }

    public void addLabel(final Label label) {
        node.addLabel(label);
        invalidateCache();
    }

    public void setHourProp(final Integer hour) {
        node.setProperty(HOUR.getText(), hour);
        invalidateCache();
    }

    public void setTime(final TramTime tramTime) {
        setTime(tramTime, node);
        invalidateCache();
    }

    public void set(final Station station) {
        set(station, node);
        invalidateCache();
    }

    public void set(final Platform platform) {
        set(platform, node);
        invalidateCache();
    }

    public void set(final Route route) {
        set(route, node);
        invalidateCache();
    }

    public void set(final Service service) {
        set(service, node);
        invalidateCache();
    }

    public void set(final StationLocalityGroup stationGroup) {
        set(stationGroup, node);
        invalidateCache();
    }

    public void set(final RouteStation routeStation) {
        set(routeStation, node);
        invalidateCache();
    }

    public void setTransportMode(final TransportMode first) {
        node.setProperty(TRANSPORT_MODE.getText(), first.getNumber());
        invalidateCache();
    }

    public void set(final DataSourceInfo nameAndVersion) {
        DataSourceID sourceID = nameAndVersion.getID();
        node.setProperty(sourceID.name(), nameAndVersion.getVersion());
        invalidateCache();
    }

    public void setLatLong(final LatLong latLong) {
        node.setProperty(LATITUDE.getText(), latLong.getLat());
        node.setProperty(LONGITUDE.getText(), latLong.getLon());
        invalidateCache();
    }

    public void setBounds(final BoundingBox bounds) {
        node.setProperty(MAX_EASTING.getText(), bounds.getMaxEasting());
        node.setProperty(MAX_NORTHING.getText(), bounds.getMaxNorthings());
        node.setProperty(MIN_EASTING.getText(), bounds.getMinEastings());
        node.setProperty(MIN_NORTHING.getText(), bounds.getMinNorthings());
        invalidateCache();
    }

    public void setWalkId(final LatLong origin, final UUID uid) {
        node.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
        invalidateCache();
    }

    public void setPlatformNumber(final Platform platform) {
        node.setProperty(PLATFORM_NUMBER.getText(), platform.getPlatformNumber());
        invalidateCache();
    }

    public void setSourceName(final String sourceName) {
        node.setProperty(SOURCE_NAME_PROP.getText(), sourceName);
        invalidateCache();
    }

    public void setAreaId(final IdFor<NPTGLocality> localityId) {
        node.setProperty(AREA_ID.getText(), localityId.getGraphId());
        invalidateCache();
    }

    public void setTowards(final IdFor<Station> stationId) {
        node.setProperty(TOWARDS_STATION_ID.getText(), stationId.getGraphId());
        invalidateCache();
    }

    ///// GET //////////////////////////////////////////////////

    // NOTE: Transaction closed exceptions will occur if keep reference to node beyond lifetime of the original transaction

    public EnumSet<GraphLabel> getLabels() {
        return GraphLabel.from(node.getLabels());
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                               final TransportRelationshipTypes relationshipType) {
        return node.getRelationships(map(direction), relationshipType).stream().map(txn::wrapRelationship);
    }

    private Direction map(final GraphDirection direction) {
        return switch (direction) {
            case Outgoing -> Direction.OUTGOING;
            case Incoming -> Direction.INCOMING;
            case Both -> Direction.BOTH;
        };
    }

    public Stream<MutableGraphRelationship> getRelationshipsMutable(final MutableGraphTransactionNeo4J txn, final GraphDirection direction,
                                                                    final TransportRelationshipTypes relationshipType) {
        return node.getRelationships(map(direction), relationshipType).stream().map(txn::wrapRelationshipMutable);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                               final TransportRelationshipTypes... transportRelationshipTypes) {
        return node.getRelationships(map(direction), transportRelationshipTypes).stream().map(txn::wrapRelationship);
    }

    @Override
    public boolean hasOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE).
                anyMatch(relationship -> relationship.hasTripIdInList(tripId));
    }

    @Override
    public Stream<ImmutableGraphRelationship> getOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE).
                filter(relationship -> relationship.hasTripIdInList(tripId));
    }

    @Override
    public TransportMode getTransportMode() {
        short number = (short) super.getProperty(TRANSPORT_MODE, node);
        return TransportMode.fromNumber(number);
    }

    @Override
    public int getHour() {
        return GraphLabel.getHourFrom(getLabels());
    }

    @Override
    public boolean hasRelationship(final GraphDirection direction, final TransportRelationshipTypes transportRelationshipTypes) {
        return node.hasRelationship(map(direction), transportRelationshipTypes);
    }

    @Override
    public boolean hasLabel(final GraphLabel graphLabel) {
        return node.hasLabel(graphLabel);
    }

    @Override
    public ImmutableGraphRelationship getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipType,
                                                            GraphDirection direction) {
        final Relationship found = node.getSingleRelationship(transportRelationshipType, map(direction));
        if (found==null) {
            return null;
        }
        return txn.wrapRelationship(found);
    }

    public MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransactionNeo4J txn, TransportRelationshipTypes transportRelationshipType,
                                                                 GraphDirection direction) {
        final Relationship found = node.getSingleRelationship(transportRelationshipType, map(direction));
        if (found==null) {
            return null;
        }
        return txn.wrapRelationshipMutable(found);
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
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return getIdFor(StationLocalityGroup.class, node);
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

    public BoundingBox getBounds() {
        int minEasting = (int) node.getProperty(MIN_EASTING.getText());
        int minNorthing = (int) node.getProperty(MIN_NORTHING.getText());

        int maxEasting = (int) node.getProperty(MAX_EASTING.getText());
        int maxNorthing = (int) node.getProperty(MAX_NORTHING.getText());
        return new BoundingBox(minEasting, minNorthing, maxEasting, maxNorthing);
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
