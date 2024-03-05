package com.tramchester.graph.search;

import com.tramchester.domain.*;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;

import java.util.Set;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(GraphTransaction txn, Location<?> startStation, Location<?> destination, JourneyRequest journeyRequest, Running running);

    Stream<Journey> calculateRouteWalkAtEnd(GraphTransaction txn, Location<?> start, GraphNode destination, LocationCollection destStations,
                                            JourneyRequest journeyRequest, NumberOfChanges numberOfChanges, Running running);

    Stream<Journey> calculateRouteWalkAtStart(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startOfWalkNode, Location<?> destination,
                                              JourneyRequest journeyRequest, NumberOfChanges numberOfChanges, Running running);

    Stream<Journey> calculateRouteWalkAtStartAndEnd(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startNode, GraphNode endNode,
                                                    LocationCollection destinationStations,
                                                    JourneyRequest journeyRequest, NumberOfChanges numberOfChanges, Running running);
}
