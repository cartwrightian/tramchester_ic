package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;

public interface JourneyStateUpdate {
    void board(TransportMode transportMode, GraphNode node, boolean hasPlatform) throws TramchesterException;
    void leave(TransportMode mode, TramDuration totalCost, GraphNode node) throws TramchesterException;

    void beginTrip(IdFor<Trip> newTripId);

    void beginWalk(GraphNode beforeWalkNode, boolean atStart, TramDuration cost);
    void endWalk(GraphNode stationNode);

    void toNeighbour(GraphNode startNode, GraphNode endNode, TramDuration cost);
    void recordStation(IdFor<Station> stationId);

    void updateTotalCost(TramDuration total);
    void recordTime(TramTime time, TramDuration totalCost) throws TramchesterException;

    void recordRouteStation(GraphNode node);

    void recordStationGroup(IdFor<StationLocalityGroup> stationGroupId);

    void beginDiversion(final IdFor<Station> stationId);
    boolean onDiversion();

    boolean onTrip();
    IdFor<Trip> getCurrentTrip();

    boolean alreadyPassed(IdFor<Station> stationId);
}
