package com.tramchester.graph.facade;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

public interface GraphNode {

    GraphNodeId getId();

    Map<String, Object> getAllProperties();

    Traverser getTraverserFor(TraversalDescription traversalDesc);

    boolean hasRelationship(Direction direction, TransportRelationshipTypes transportRelationshipTypes);

    boolean hasLabel(GraphLabel graphLabel);

    GraphRelationship getSingleRelationship(MutableGraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, Direction direction);

    IdFor<RouteStation> getRouteStationId();

    IdFor<Service> getServiceId();

    IdFor<Trip> getTripId();

    TramTime getTime();

    LatLong getLatLong();

    boolean hasTripId();

    PlatformId getPlatformId();

    boolean hasStationId();

    EnumSet<GraphLabel> getLabels();

    IdFor<Station> getStationId();

    Stream<ImmutableGraphRelationship> getRelationships(GraphTransaction txn, Direction direction, TransportRelationshipTypes relationshipType);

    Stream<ImmutableGraphRelationship> getRelationships(GraphTransaction txn, Direction direction, TransportRelationshipTypes... transportRelationshipTypes);

    TransportMode getTransportMode();

    Integer getHour();

    IdFor<Route> getRouteId();

    IdFor<NaptanArea> getAreaId();

}
