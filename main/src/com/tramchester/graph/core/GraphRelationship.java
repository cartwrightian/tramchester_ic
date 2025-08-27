package com.tramchester.graph.core;

import com.tramchester.domain.CoreDomain;
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
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.time.Duration;
import java.util.EnumSet;

public interface GraphRelationship extends GraphEntity {

    GraphRelationshipId getId();

    TramTime getTime();

    int getHour();

    Duration getCost();

    GraphNode getEndNode(final GraphTransaction txn);

    GraphNode getStartNode(GraphTransaction txn);

    GraphNodeId getStartNodeId(GraphTransaction txn);

    GraphNodeId getEndNodeId(GraphTransaction txn);

    EnumSet<TransportMode> getTransportModes() ;

    TransportRelationshipTypes getType();

    IdFor<Route> getRouteId();

    IdFor<Service> getServiceId();

    IdFor<Trip> getTripId();

    boolean isType(TransportRelationshipTypes transportRelationshipType);

    IdFor<RouteStation> getRouteStationId();

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

    boolean hasTripIdInList(IdFor<Trip> tripId);

    default IdFor<? extends CoreDomain> getStart(final GraphTransaction txn) {
        final GraphNode node = getStartNode(txn);
        return node.getCoreDomainId();
    }

    default IdFor<? extends CoreDomain> getEnd(final GraphTransaction txn) {
        final GraphNode node = getEndNode(txn);
        return node.getCoreDomainId();
    }
}
