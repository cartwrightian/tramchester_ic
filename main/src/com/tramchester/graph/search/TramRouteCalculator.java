package com.tramchester.graph.search;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphTransaction;

import java.util.Set;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(ImmutableGraphTransaction txn, Location<?> start, Location<?> destination, JourneyRequest journeyRequest, Running running);

    Stream<Journey> calculateRouteWalkAtEnd(ImmutableGraphTransaction txn, Location<?> start, GraphNode destination, LocationCollection destStations,
                                            JourneyRequest journeyRequest, int possibleMinChanges, Running running);

    Stream<Journey> calculateRouteWalkAtStart(ImmutableGraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startOfWalkNode, Location<?> destination,
                                              JourneyRequest journeyRequest, int possibleMinChanges, Running running);

    Stream<Journey> calculateRouteWalkAtStartAndEnd(ImmutableGraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startNode, GraphNode endNode,
                                                    LocationCollection destinationStations,
                                                    JourneyRequest journeyRequest, int possibleMinChanges, Running running);
}
