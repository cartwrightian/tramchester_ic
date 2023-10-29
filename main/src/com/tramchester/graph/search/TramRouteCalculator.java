package com.tramchester.graph.search;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.graph.GraphNode;
import org.neo4j.graphdb.Transaction;

import java.util.Set;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(Transaction txn, Location<?> startStation, Location<?> destination, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Location<?> start, GraphNode destination, LocationSet destStations,
                                            JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);

    Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Set<StationWalk> stationWalks, GraphNode startOfWalkNode, Location<?> destination,
                                              JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);

    Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Set<StationWalk> stationWalks, GraphNode startNode, GraphNode endNode,
                                                    LocationSet destinationStations,
                                                    JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);
}
