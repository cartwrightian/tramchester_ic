package com.tramchester.graph.search;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;

import java.util.Set;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(MutableGraphTransaction txn, Location<?> startStation, Location<?> destination, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtEnd(MutableGraphTransaction txn, Location<?> start, GraphNode destination, LocationSet destStations,
                                            JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);

    Stream<Journey> calculateRouteWalkAtStart(MutableGraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startOfWalkNode, Location<?> destination,
                                              JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);

    Stream<Journey> calculateRouteWalkAtStartAndEnd(MutableGraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startNode, GraphNode endNode,
                                                    LocationSet destinationStations,
                                                    JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);
}
