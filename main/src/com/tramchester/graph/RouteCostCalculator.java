package com.tramchester.graph;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.neo4j.RouteCostCalculatorNeo4J;

import java.time.Duration;
import java.util.EnumSet;

@ImplementedBy(RouteCostCalculatorNeo4J.class)
public interface RouteCostCalculator {
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
