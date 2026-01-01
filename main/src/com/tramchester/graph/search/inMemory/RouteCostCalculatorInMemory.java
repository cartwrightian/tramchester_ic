package com.tramchester.graph.search.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.repository.RouteRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class RouteCostCalculatorInMemory implements RouteCostCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostCalculatorInMemory.class);

    private final RouteRepository routeRepository;
    private final TramchesterConfig config;

    @Inject
    public RouteCostCalculatorInMemory(StagedTransportGraphBuilder.Ready ready, RouteRepository routeRepository, TramchesterConfig config) {
        this.routeRepository = routeRepository;
        this.config = config;
    }

    @Override
    public TramDuration getAverageCostBetween(GraphTransaction txn, GraphNode startNode, GraphNode endNode, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        return calculateLeastCost(txn, startNode, endNode, date, modes);
    }

    @Override
    public TramDuration getAverageCostBetween(GraphTransaction txn, Location<?> station, GraphNode endNode, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        final GraphNode startNode = txn.findNode(station);
        return calculateLeastCost(txn, startNode, endNode, date, modes);
    }

    @Override
    public TramDuration getAverageCostBetween(GraphTransaction txn, GraphNode startNode, Location<?> endStation, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        final GraphNode endNode = txn.findNode(endStation);
        return calculateLeastCost(txn, startNode, endNode, date, modes);
    }

    @Override
    public TramDuration getAverageCostBetween(GraphTransaction txn, Location<?> startStation, Location<?> endStation, TramDate date, EnumSet<TransportMode> modes) throws InvalidDurationException {
        return getCostBetween(txn, startStation, endStation, date, modes);
    }

    private TramDuration getCostBetween(final GraphTransaction txn, final Location<?> startLocation, final Location<?> endLocation,
                                    final TramDate date, final EnumSet<TransportMode> modes) throws InvalidDurationException {
        final GraphNode startNode = txn.findNode(startLocation);
        if (startNode==null) {
            throw new RuntimeException("Could not find start node for graph id " + startLocation.getId().getGraphId());
        }
        final GraphNode endNode = txn.findNode(endLocation);
        if (endNode==null) {
            throw new RuntimeException("Could not find end node for graph id" + endLocation.getId().getGraphId());
        }
        logger.info(format("Find approx. route cost between %s and %s", startLocation.getId(), endLocation.getId()));

        return calculateLeastCost(txn, startNode, endNode, date, modes);
    }

    // startNode and endNode must have been found within supplied txn
    private TramDuration calculateLeastCost(final GraphTransaction txn, final GraphNode startNode, final GraphNode endNode,
                                        final TramDate date, final EnumSet<TransportMode> modes) throws InvalidDurationException {

        final Set<Route> routesRunningOn = routeRepository.getRoutesRunningOn(date, modes).stream().
                filter(route -> modes.contains(route.getTransportMode())).collect(Collectors.toSet());

        final IdSet<Route> available = IdSet.from(routesRunningOn);

        final ShortestPath findPathsForJourney = new ShortestPath(txn, startNode);

        final FindPathsForJourney.GraphRelationshipFilter routeAvailableFilter = relationship -> {
            final TransportRelationshipTypes relationshipType = relationship.getType();
            if (RouteCostCalculator.costApproxTypes.contains(relationshipType)) {
                if (relationshipType==TransportRelationshipTypes.ON_ROUTE) {
                    return available.contains(relationship.getRouteId());
                }
                return true;
            }
            return false;
        };

        final TramDuration result = findPathsForJourney.findShortestPathsTo(endNode, routeAvailableFilter);

        if (result.equals(FindPathsForJourney.NotVisitiedDuration)) {
            final String message = format("No (least cost) path found between node %s [%s] and node %s [%s]",
                    startNode.getId(), startNode.getAllProperties(), endNode.getId(), endNode.getAllProperties());
            logger.error(message);
            throw new InvalidDurationException(message);
        }

        return result;

    }
}
