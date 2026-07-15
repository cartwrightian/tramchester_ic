package com.tramchester.graph.core;

import com.tramchester.domain.*;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
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
import com.tramchester.graph.reference.GraphLabels;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.Map;
import java.util.stream.Stream;

public interface GraphNode extends GraphEntity<GraphNodeId> {

    GraphNodeId getId();

    boolean hasLabel(GraphLabel graphLabel);
    GraphLabels getLabels();

    boolean hasRelationship(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes transportRelationshipTypes);
    boolean hasRelationship(GraphTransaction txn, GraphDirection graphDirection, TransportRelationshipTypes relationshipType, GraphNode end);

    GraphRelationship getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipTypes, GraphDirection direction);
    Stream<GraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, TransportRelationshipTypes relationshipType);
    Stream<GraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction, ImmutableEnumSet<TransportRelationshipTypes> types);
    Stream<GraphRelationship> getAllRelationships(GraphTransaction txn, GraphDirection direction);

    String getUniqueWalkId();

    Map<DataSourceID, String> getStoredVersions();

    IdFor<RouteStation> getRouteStationId();

    IdFor<Service> getServiceId();

    IdFor<Trip> getTripId();

    boolean hasOutgoingServiceMatching(GraphTransaction txn, IdFor<Trip> tripId);
    Stream<GraphRelationship> getOutgoingServiceMatching(GraphTransaction txn, IdFor<Trip> tripId);

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

    TramDate getStartDate();

    LatLong getLatLong();

    TransportMode getTransportMode();

    int getHour();


}
