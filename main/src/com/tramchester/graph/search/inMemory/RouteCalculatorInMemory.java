package com.tramchester.graph.search.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphPath;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.TramRouteCalculator;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Stream;

@LazySingleton
public class RouteCalculatorInMemory implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorInMemory.class);

    private final TramchesterConfig config;

    @Inject
    public RouteCalculatorInMemory(TramchesterConfig config) {
        this.config = config;
    }

    @Override
    public Stream<Journey> calculateRoute(final GraphTransaction txn, final Location<?> start, final Location<?> destination,
                                          final JourneyRequest journeyRequest, Running running) {
        GraphNode startNode = getLocationNodeSafe(txn, start);
        GraphNode destinationNode = getLocationNodeSafe(txn, destination);

        SpikeAlgo spikeAlgo = new SpikeAlgo(txn, startNode, destinationNode, config);

        GraphPath path = spikeAlgo.findRoute();

        return Stream.empty();
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(GraphTransaction txn, Location<?> start, GraphNode destination, LocationCollection destStations, JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startOfWalkNode, Location<?> destination, JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStartAndEnd(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startNode, GraphNode endNode, LocationCollection destinationStations, JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        throw new RuntimeException("Not implemented");
    }

    protected GraphNode getLocationNodeSafe(final GraphTransaction txn, final Location<?> location) {
        final GraphNode findNode = txn.findNode(location);
        if (findNode == null) {
            String msg = "Unable to find node for " + location.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        logger.info("found node " + findNode.getId() + " for " + location.getId());
        return findNode;
    }
}
