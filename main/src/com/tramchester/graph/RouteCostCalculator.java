package com.tramchester.graph;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.time.Duration;
import java.util.EnumSet;

import static com.tramchester.graph.reference.TransportRelationshipTypes.*;

public interface RouteCostCalculator {

    EnumSet<TransportRelationshipTypes> costApproxTypes = EnumSet.of(
            ON_ROUTE,
            STATION_TO_ROUTE,
            ROUTE_TO_STATION,
            WALKS_TO_STATION,
            WALKS_FROM_STATION,
            NEIGHBOUR,
            GROUPED_TO_PARENT,
            GROUPED_TO_GROUPED,
            GROUPED_TO_CHILD);

    Duration getAverageCostBetween(GraphTransaction txn, GraphNode startNode, GraphNode endNode,
                                   TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException;

    Duration getAverageCostBetween(GraphTransaction txn, Location<?> station, GraphNode endNode, TramDate date,
                                   EnumSet<TransportMode> modes) throws InvalidDurationException;

    // startNode must have been found within supplied txn
    Duration getAverageCostBetween(GraphTransaction txn, GraphNode startNode, Location<?> endStation,
                                   TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException;

    Duration getAverageCostBetween(GraphTransaction txn, Location<?> startStation, Location<?> endStation,
                                   TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException;
}
