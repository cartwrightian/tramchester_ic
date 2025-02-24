package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;

import java.time.Duration;

public interface JourneyStateUpdate {
    void board(TransportMode transportMode, GraphNode node, boolean hasPlatform) throws TramchesterException;
    void leave(TransportMode mode, Duration totalCost, GraphNode node) throws TramchesterException;

    void beginTrip(IdFor<Trip> newTripId);

    void beginWalk(GraphNode beforeWalkNode, boolean atStart, Duration cost);
    void endWalk(GraphNode stationNode);

    void toNeighbour(GraphNode startNode, GraphNode endNode, Duration cost);
    void seenStation(IdFor<Station> stationId);

    void updateTotalCost(Duration total);
    void recordTime(TramTime time, Duration totalCost) throws TramchesterException;

    void seenRouteStation(IdFor<Station> correspondingStationId);

    void seenStationGroup(IdFor<StationLocalityGroup> stationGroupId);

    void beginDiversion(final IdFor<Station> stationId);
    boolean onDiversion();

    boolean onTrip();
    IdFor<Trip> getCurrentTrip();
}
