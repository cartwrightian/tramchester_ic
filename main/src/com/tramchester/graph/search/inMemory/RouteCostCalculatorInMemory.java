package com.tramchester.graph.search.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.EnumSet;

@LazySingleton
public class RouteCostCalculatorInMemory implements RouteCostCalculator {

    @Inject
    public RouteCostCalculatorInMemory(StagedTransportGraphBuilder.Ready ready) {

    }

    @Override
    public Duration getAverageCostBetween(GraphTransaction txn, GraphNode startNode, GraphNode endNode, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Duration getAverageCostBetween(GraphTransaction txn, Location<?> station, GraphNode endNode, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Duration getAverageCostBetween(GraphTransaction txn, GraphNode startNode, Location<?> endStation, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Duration getAverageCostBetween(GraphTransaction txn, Location<?> startStation, Location<?> endStation, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        throw new RuntimeException("Not implemented");
    }
}
