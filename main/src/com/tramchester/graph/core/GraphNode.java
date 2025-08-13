package com.tramchester.graph.core;

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
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

public interface GraphNode extends GraphEntity {

    GraphNodeId getId();

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

    boolean hasRelationship(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes transportRelationshipTypes);

    GraphRelationship getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, GraphDirection direction);

    Stream<GraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes relationshipType);

    Stream<GraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, EnumSet<TransportRelationshipTypes> types);

    Stream<GraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes... transportRelationshipTypes);

    boolean hasOutgoingServiceMatching(GraphTransaction txn, IdFor<Trip> tripId);

    Stream<GraphRelationship> getOutgoingServiceMatching(GraphTransaction txn, IdFor<Trip> tripId);

//    Stream<GraphRelationship> getAllRelationships(GraphTransaction txn, GraphDirection direction);

}
