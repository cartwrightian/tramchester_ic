package com.tramchester.graph.facade;

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
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.neo4j.GraphNodeId;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

public interface GraphNode extends GraphEntity {

    GraphNodeId getId();

    Traverser getTraverserFor(TraversalDescription traversalDesc);

    IdFor<RouteStation> getRouteStationId();

    IdFor<Service> getServiceId();

    IdFor<Trip> getTripId();

    IdFor<Platform> getPlatformId();

    IdFor<Station> getStationId();

    IdFor<Station> getTowardsStationId();

    IdFor<Route> getRouteId();

    IdFor<StationLocalityGroup> getStationGroupId();

    IdFor<NPTGLocality> getAreaId();

    BoundingBox getBounds();

    boolean hasTripId();

    boolean hasStationId();

    TramTime getTime();

    LatLong getLatLong();

    TransportMode getTransportMode();

    int getHour();

    boolean hasLabel(GraphLabel graphLabel);

    EnumSet<GraphLabel> getLabels();

    Map<String, Object> getAllProperties();

    boolean hasRelationship(GraphDirection direction, TransportRelationshipTypes transportRelationshipTypes);

    ImmutableGraphRelationship getSingleRelationship(GraphTransactionNeo4J txn, TransportRelationshipTypes transportRelationshipTypes, GraphDirection direction);

    Stream<ImmutableGraphRelationship> getRelationships(GraphTransactionNeo4J txn, GraphDirection direction, TransportRelationshipTypes relationshipType);

    Stream<ImmutableGraphRelationship> getRelationships(GraphTransactionNeo4J txn, GraphDirection direction, TransportRelationshipTypes... transportRelationshipTypes);

    boolean hasOutgoingServiceMatching(GraphTransactionNeo4J txn, IdFor<Trip> tripId);

    Stream<ImmutableGraphRelationship> getOutgoingServiceMatching(GraphTransactionNeo4J txn, IdFor<Trip> tripId);
}
