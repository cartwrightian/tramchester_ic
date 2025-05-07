package com.tramchester.graph.facade;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
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
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class ImmutableGraphNode implements GraphNode {
    private final MutableGraphNode underlying;
    private final GraphNodeId nodeId;

    // Node properties only, nodes cannot be shared beyond parent transactions
    private final SharedNodeCache sharedNodeCache;

    ImmutableGraphNode(final MutableGraphNode underlying, SharedNodeCache sharedNodeCache) {
        this.underlying = underlying;
        this.nodeId = underlying.getId();
        this.sharedNodeCache = sharedNodeCache;
    }

    public static WeightedPath findSinglePath(PathFinder<WeightedPath> finder, GraphNode startNode, GraphNode endNode) {
        final Node start = getNodeFor(startNode);
        final Node end = getNodeFor(endNode);
        return finder.findSinglePath(start, end);
    }

    private static Node getNodeFor(final GraphNode graphNode) {
        if (graphNode instanceof ImmutableGraphNode) {
            return ((ImmutableGraphNode)graphNode).getNode();
        }
        return ((MutableGraphNode)graphNode).getNode();
    }

    private Node getNode() {
        return underlying.getNode();
    }

    @Override
    public GraphNodeId getId() {
        return underlying.getId();
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return underlying.getAllProperties();
    }

    @Override
    public Traverser getTraverserFor(TraversalDescription traversalDesc) {
        return underlying.getTraverserFor(traversalDesc);
    }

    @Override
    public boolean hasRelationship(final Direction direction, final TransportRelationshipTypes transportRelationshipTypes) {
        return underlying.hasRelationship(direction, transportRelationshipTypes);
    }

    @Override
    public boolean hasLabel(final GraphLabel graphLabel) {
        return sharedNodeCache.hasLabel(nodeId, graphLabel, nodeId -> underlying.getLabels());
    }

    @Override
    public ImmutableGraphRelationship getSingleRelationship(final GraphTransaction txn,
                                                            final TransportRelationshipTypes transportRelationshipTypes, final Direction direction) {
        return underlying.getSingleRelationship(txn, transportRelationshipTypes,direction);
    }

    @Override
    public IdFor<RouteStation> getRouteStationId() {
        return sharedNodeCache.getRouteStationId(nodeId, nodeId -> underlying.getRouteStationId());
    }

    @Override
    public IdFor<Service> getServiceId() {
        return sharedNodeCache.getServiceId(nodeId, nodeId -> underlying.getServiceId());
    }

    @Override
    public IdFor<Trip> getTripId() {
        return sharedNodeCache.getTripId(nodeId, nodeId -> underlying.getTripId());
    }

    @Override
    public TramTime getTime() {
        return sharedNodeCache.getTime(nodeId, k -> underlying.getTime());
    }

    @Override
    public LatLong getLatLong() {
        return underlying.getLatLong();
    }

    @Override
    public boolean hasTripId() {
        return underlying.hasTripId();
    }

    @Override
    public PlatformId getPlatformId() {
        return underlying.getPlatformId();
    }

    @Override
    public boolean hasStationId() {
        return underlying.hasStationId();
    }

    @Override
    public EnumSet<GraphLabel> getLabels() {
        return sharedNodeCache.getLabels(nodeId, nodeId -> underlying.getLabels());
    }

    @Override
    public IdFor<Station> getStationId() {
        return sharedNodeCache.getStationId(nodeId, nodeId -> underlying.getStationId());
    }

    @Override
    public IdFor<Station> getTowardsStationId() {
        return underlying.getTowardsStationId();
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final Direction direction,
                                                               final TransportRelationshipTypes relationshipType) {
        return underlying.getRelationships(txn, direction, relationshipType);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final Direction direction,
                                                               final TransportRelationshipTypes... transportRelationshipTypes) {
        return underlying.getRelationships(txn, direction, transportRelationshipTypes);
    }

    @Override
    public boolean hasOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return underlying.hasOutgoingServiceMatching(txn, tripId);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return underlying.getOutgoingServiceMatching(txn, tripId);
    }

    @Override
    public TransportMode getTransportMode() {
        return underlying.getTransportMode();
    }

    @Override
    public int getHour() {
        return sharedNodeCache.getHour(nodeId, k -> underlying.getHour());
    }

    @Override
    public IdFor<Route> getRouteId() {
        return underlying.getRouteId();
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return underlying.getStationGroupId();
    }

    @Override
    public IdFor<NPTGLocality> getAreaId() {
        return underlying.getAreaId();
    }

    @Override
    public BoundingBox getBounds() {
        return underlying.getBounds();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> klass = o.getClass();
        if (!GraphNode.class.isAssignableFrom(klass)) return false;
        GraphNode that = (GraphNode)o;
        return Objects.equals(nodeId, that.getId());
    }

    @Override
    public String toString() {
        return "ImmutableGraphNode{" +
                "underlying=" + underlying +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

}
