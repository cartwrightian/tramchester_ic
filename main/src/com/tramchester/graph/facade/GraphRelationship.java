package com.tramchester.graph.facade;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.neo4j.GraphNodeId;
import com.tramchester.graph.facade.neo4j.GraphRelationshipId;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;

public interface GraphRelationship {

    GraphRelationshipId getId();

    TramTime getTime();

    int getHour();

    Duration getCost();

    GraphNode getEndNode(final GraphTransactionNeo4J txn);

    GraphNode getStartNode(GraphTransactionNeo4J txn);

    GraphNodeId getStartNodeId(ImmutableGraphTransactionNeo4J txn);

    GraphNodeId getEndNodeId(GraphTransactionNeo4J txn);

    EnumSet<TransportMode> getTransportModes() ;

    TransportRelationshipTypes getType();

    IdFor<Route> getRouteId();

    IdFor<Service> getServiceId();

    IdFor<Trip> getTripId();

    boolean isType(TransportRelationshipTypes transportRelationshipType);

    IdFor<RouteStation> getRouteStationId();

    Map<String,Object> getAllProperties();

    boolean isDayOffset();

    boolean validOn(TramDate tramDate);

    IdFor<Station> getStationId();

    boolean hasProperty(GraphPropertyKey graphPropertyKey);

    int getStopSeqNumber();

    IdFor<Station> getEndStationId();

    IdFor<Station> getStartStationId();

    IdFor<StationLocalityGroup> getStationGroupId();

    IdSet<Trip> getTripIds();

    DateRange getDateRange();

    TimeRange getTimeRange();

    DateTimeRange getDateTimeRange();

    TramTime getStartTime();

    TramTime getEndTime();

    LocationId<?> getLocationId();
}
