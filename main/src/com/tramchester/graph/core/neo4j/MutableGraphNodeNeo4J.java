package com.tramchester.graph.core.neo4j;

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
import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.*;
import static com.tramchester.graph.reference.TransportRelationshipTypes.TO_SERVICE;

public class MutableGraphNodeNeo4J extends HaveGraphProperties<KeyValuePropsNeo4J> implements MutableGraphNode, CreateGraphTraverser {
    private final Node node;
    private final GraphNodeId graphNodeId;
    private final GraphReferenceMapper relationshipTypeFactory;
    private final SharedNodeCache.InvalidatesCacheForNode invalidatesCacheForNode;
    private final KeyValuePropsNeo4J entity;

    MutableGraphNodeNeo4J(Node node, GraphNodeId graphNodeId, GraphReferenceMapper relationshipTypeFactory, SharedNodeCache.InvalidatesCacheForNode invalidatesCacheForNode) {
        this.relationshipTypeFactory = relationshipTypeFactory;
        this.invalidatesCacheForNode = invalidatesCacheForNode;
        if (node == null) {
            throw new RuntimeException("Null node passed");
        }
        this.node = node;
        this.graphNodeId = graphNodeId;
        this.entity = KeyValuePropsNeo4J.wrap(node);
    }

    public GraphNodeId getId() {
        return graphNodeId;
    }

    Node getNode() {
        return node;
    }

    @Override
    public void delete() {
        invalidateCache();
        node.delete();
    }

    private void invalidateCache() {
        invalidatesCacheForNode.remove();
    }

    ///// MUTATE ////////////////////////////////////////////////////////////

    @Override
    public MutableGraphRelationship createRelationshipTo(final MutableGraphTransaction txn, final MutableGraphNode end,
                                                         final TransportRelationshipTypes relationshipType) {

        // TODO address casting, here and elsewhere
        final MutableGraphTransactionNeo4J txnNeo4J = (MutableGraphTransactionNeo4J) txn;
        final MutableGraphNodeNeo4J endNode = (MutableGraphNodeNeo4J) end;

        final Relationship relationshipTo = node.createRelationshipTo(endNode.node, relationshipTypeFactory.get(relationshipType));
        return txnNeo4J.wrapRelationshipMutable(relationshipTo);
    }

    @Override
    public void addLabel(final GraphLabel graphLabel) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        node.addLabel(label);
        invalidateCache();
    }

    @Override
    public void setHourProp(final Integer hour) {
        node.setProperty(HOUR.getText(), hour);
        invalidateCache();
    }

    @Override
    public void setTime(final TramTime tramTime) {
        setTime(tramTime, entity);
        invalidateCache();
    }

    @Override
    public void set(final Station station) {
        set(station, entity);
        invalidateCache();
    }

    @Override
    public void set(final Platform platform) {
        set(platform, entity);
        invalidateCache();
    }

    @Override
    public void set(final Route route) {
        set(route, entity);
        invalidateCache();
    }

    @Override
    public void set(final Service service) {
        set(service, entity);
        invalidateCache();
    }

    @Override
    public void set(final StationLocalityGroup stationGroup) {
        set(stationGroup, entity);
        invalidateCache();
    }

    @Override
    public void set(final RouteStation routeStation) {
        set(routeStation, entity);
        invalidateCache();
    }

    @Override
    public void setTransportMode(final TransportMode first) {
        node.setProperty(TRANSPORT_MODE.getText(), first.getNumber());
        invalidateCache();
    }

    @Override
    public void set(final DataSourceInfo nameAndVersion) {
        final DataSourceID sourceID = nameAndVersion.getID();
        node.setProperty(sourceID.name(), nameAndVersion.getVersion());
        invalidateCache();
    }

    @Override
    public void setLatLong(final LatLong latLong) {
        node.setProperty(LATITUDE.getText(), latLong.getLat());
        node.setProperty(LONGITUDE.getText(), latLong.getLon());
        invalidateCache();
    }

    @Override
    public void setBounds(final BoundingBox bounds) {
        node.setProperty(MAX_EASTING.getText(), bounds.getMaxEasting());
        node.setProperty(MAX_NORTHING.getText(), bounds.getMaxNorthings());
        node.setProperty(MIN_EASTING.getText(), bounds.getMinEastings());
        node.setProperty(MIN_NORTHING.getText(), bounds.getMinNorthings());
        invalidateCache();
    }

    @Override
    public void setWalkId(final LatLong origin, final UUID uid) {
        node.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
        invalidateCache();
    }

    @Override
    public void setPlatformNumber(final Platform platform) {
        node.setProperty(PLATFORM_NUMBER.getText(), platform.getPlatformNumber());
        invalidateCache();
    }

    @Override
    public void setSourceName(final String sourceName) {
        node.setProperty(SOURCE_NAME_PROP.getText(), sourceName);
        invalidateCache();
    }

    @Override
    public void setAreaId(final IdFor<NPTGLocality> localityId) {
        node.setProperty(AREA_ID.getText(), localityId.getGraphId());
        invalidateCache();
    }

    @Override
    public void setTowards(final IdFor<Station> stationId) {
        node.setProperty(TOWARDS_STATION_ID.getText(), stationId.getGraphId());
        invalidateCache();
    }

    ///// GET //////////////////////////////////////////////////

    // NOTE: Transaction closed exceptions will occur if keep reference to node beyond lifetime of the original transaction

    @Override
    public EnumSet<GraphLabel> getLabels() {
        return GraphReferenceMapper.from(node.getLabels());
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                               final TransportRelationshipTypes relationshipType) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return node.getRelationships(map(direction), relationshipTypeFactory.get(relationshipType)).
                stream().
                map(txnNeo4J::wrapRelationship);
    }

    private Direction map(final GraphDirection direction) {
        return switch (direction) {
            case Outgoing -> Direction.OUTGOING;
            case Incoming -> Direction.INCOMING;
            case Both -> Direction.BOTH;
        };
    }

    @Override
    public Stream<MutableGraphRelationship> getRelationshipsMutable(final MutableGraphTransaction txn, final GraphDirection direction,
                                                                    final TransportRelationshipTypes relationshipType) {
        MutableGraphTransactionNeo4J txnNeo4J = (MutableGraphTransactionNeo4J) txn;
        return node.getRelationships(map(direction), relationshipTypeFactory.get(relationshipType)).stream().map(txnNeo4J::wrapRelationshipMutable);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                                    final TransportRelationshipTypes... transportRelationshipTypes) {
        GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        RelationshipType[] relationshipTypes =  relationshipTypeFactory.get(transportRelationshipTypes);
        return node.getRelationships(map(direction), relationshipTypes).stream().map(txnNeo4J::wrapRelationship);
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
        short number = (short) super.getProperty(TRANSPORT_MODE, entity);
        return TransportMode.fromNumber(number);
    }

    @Override
    public int getHour() {
        return GraphLabel.getHourFrom(getLabels());
    }

    @Override
    public boolean hasRelationship(final GraphDirection direction, final TransportRelationshipTypes transportRelationshipTypes) {
        return node.hasRelationship(map(direction), relationshipTypeFactory.get(transportRelationshipTypes));
    }

    @Override
    public boolean hasLabel(final GraphLabel graphLabel) {
        Label label = relationshipTypeFactory.get(graphLabel);
        return node.hasLabel(label);
    }

    @Override
    public ImmutableGraphRelationshipNeo4J getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipType,
                                                                 GraphDirection direction) {
        final Relationship found = node.getSingleRelationship(relationshipTypeFactory.get(transportRelationshipType), map(direction));
        if (found==null) {
            return null;
        }
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.wrapRelationship(found);
    }

    public MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction txn, TransportRelationshipTypes transportRelationshipType,
                                                                 GraphDirection direction) {
        final Relationship found = node.getSingleRelationship(relationshipTypeFactory.get(transportRelationshipType), map(direction));
        if (found==null) {
            return null;
        }
        final MutableGraphTransactionNeo4J txnNeo4J = (MutableGraphTransactionNeo4J) txn;
        return txnNeo4J.wrapRelationshipMutable(found);
    }


    <DT extends CoreDomain> Boolean hasIdFor(Class<DT> theClass) {
        return node.hasProperty(GraphPropertyKey.getFor(theClass).getText());
    }

    public IdFor<Station> getStationId() {
        return getIdFor(Station.class, entity);
    }

    @Override
    public void set(final Trip trip) {
        set(trip, entity);
    }

    public Map<String,Object> getAllProperties() {
        return getAllProperties(entity);
    }

    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(entity);
    }

    public IdFor<Service> getServiceId() {
        return getIdFor(Service.class, entity);
    }

    @Override
    public IdFor<Route> getRouteId() {
        return getIdFor(Route.class, entity);
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return getIdFor(StationLocalityGroup.class, entity);
    }

    @Override
    public IdFor<NPTGLocality> getAreaId() {
        return getIdFor(NPTGLocality.class, entity);
    }

    public IdFor<Trip> getTripId() {
        return getIdFor(Trip.class, entity);
    }

    public TramTime getTime() {
        return getTime(entity);
    }

    public LatLong getLatLong() {
        final double lat = (double) super.getProperty(LATITUDE, entity);
        final double lon = (double) super.getProperty(LONGITUDE, entity);
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
        MutableGraphNodeNeo4J graphNode = (MutableGraphNodeNeo4J) o;
        return Objects.equals(graphNodeId, graphNode.graphNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphNodeId);
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
    public Traverser getTraverser(TraversalDescription traversalDesc) {
        return traversalDesc.traverse(node);
    }
}
