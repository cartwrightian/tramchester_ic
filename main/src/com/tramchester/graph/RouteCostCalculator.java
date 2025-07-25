package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.RouteRepository;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.DoubleEvaluator;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.COST;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;

/***
 * Supports arrive-by calculations by finding an approx cost for a specific journey
 */
@LazySingleton
public class RouteCostCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostCalculator.class);

    private final GraphDatabaseNeo4J graphDatabaseService;
    private final RouteRepository routeRepository;

    @Inject
    public RouteCostCalculator(GraphDatabaseNeo4J graphDatabaseService,
                               @SuppressWarnings("unused") StagedTransportGraphBuilder.Ready ready,
                               RouteRepository routeRepository) {
        this.graphDatabaseService = graphDatabaseService;
        this.routeRepository = routeRepository;
    }

    public Duration getAverageCostBetween(final ImmutableGraphTransactionNeo4J txn, final GraphNode startNode, final GraphNode endNode,
                                          final TramDate date, final EnumSet<TransportMode> modes) throws InvalidDurationException {
        return calculateLeastCost(txn, startNode, endNode, COST, date, modes);
    }

    public Duration getAverageCostBetween(final ImmutableGraphTransactionNeo4J txn, final Location<?> station, final GraphNode endNode, final TramDate date,
                                          final EnumSet<TransportMode> modes) throws InvalidDurationException {
        final GraphNode startNode = txn.findNode(station);
        return calculateLeastCost(txn, startNode, endNode, COST, date, modes);
    }

    // startNode must have been found within supplied txn
    public Duration getAverageCostBetween(final ImmutableGraphTransactionNeo4J txn, final GraphNode startNode, final Location<?> endStation,
                                          final TramDate date, final EnumSet<TransportMode> modes) throws InvalidDurationException {
        final GraphNode endNode = txn.findNode(endStation);
        return calculateLeastCost(txn, startNode, endNode, COST, date, modes);
    }

    public Duration getAverageCostBetween(final ImmutableGraphTransactionNeo4J txn, final Location<?> startStation, final Location<?> endStation,
                                          final TramDate date, final EnumSet<TransportMode> modes) throws InvalidDurationException {
        return getCostBetween(txn, startStation, endStation, COST, date, modes);
    }

    private Duration getCostBetween(final ImmutableGraphTransactionNeo4J txn, final Location<?> startLocation, final Location<?> endLocation,
                                    final GraphPropertyKey key, final TramDate date, final EnumSet<TransportMode> modes) throws InvalidDurationException {
        final GraphNode startNode = txn.findNode(startLocation);
        if (startNode==null) {
            throw new RuntimeException("Could not find start node for graph id " + startLocation.getId().getGraphId());
        }
        final GraphNode endNode = txn.findNode(endLocation);
        if (endNode==null) {
            throw new RuntimeException("Could not find end node for graph id" + endLocation.getId().getGraphId());
        }
        logger.info(format("Find approx. route cost between %s and %s", startLocation.getId(), endLocation.getId()));

        return calculateLeastCost(txn, startNode, endNode, key, date, modes);
    }

    // startNode and endNode must have been found within supplied txn
    private Duration calculateLeastCost(final ImmutableGraphTransactionNeo4J txn, final GraphNode startNode, final GraphNode endNode, final GraphPropertyKey key,
                                        final TramDate date, final EnumSet<TransportMode> modes) throws InvalidDurationException {

        final Set<Route> routesRunningOn = routeRepository.getRoutesRunningOn(date, modes).stream().
                filter(route -> modes.contains(route.getTransportMode())).collect(Collectors.toSet());

        final IdSet<Route> available = IdSet.from(routesRunningOn);
        // TODO fetch all the relationshipIds first

        final EvaluationContext context = graphDatabaseService.createContext(txn);

        final GraphRelationshipFilter routeFilter = new GraphRelationshipFilter(txn,
                graphRelationship -> !graphRelationship.isType(ON_ROUTE) ||
                        available.contains(graphRelationship.getRouteId()));

        final PathExpander<Double> forTypesAndDirections = fullExpanderForCostApproximation(routeFilter);

        final PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections,
                new UsefulLoggingCostEvaluator(key));

        final WeightedPath path = ImmutableGraphNode.findSinglePath(finder, startNode, endNode);
        if (path==null) {
            final String message = format("No (least cost) path found between node %s [%s] and node %s [%s]",
                    startNode.getId(), startNode.getAllProperties(), endNode.getId(), endNode.getAllProperties());
            logger.error(message);
            throw new InvalidDurationException(message);
        }
        double weight  = Math.floor(path.weight());

        int seconds = (int) weight;
        return Duration.ofSeconds(seconds);
    }

    private PathExpander<Double> fullExpanderForCostApproximation(final Predicate<? super Relationship> routeFilter) {
        return PathExpanderBuilder.empty().
                add(ON_ROUTE, Direction.OUTGOING).
                add(STATION_TO_ROUTE, Direction.OUTGOING).
                add(ROUTE_TO_STATION, Direction.OUTGOING).
                add(WALKS_TO_STATION, Direction.OUTGOING).
                add(WALKS_FROM_STATION, Direction.OUTGOING).
                add(NEIGHBOUR, Direction.OUTGOING).
                add(GROUPED_TO_PARENT, Direction.OUTGOING).
                add(GROUPED_TO_GROUPED, Direction.OUTGOING).
                add(GROUPED_TO_CHILD, Direction.OUTGOING).
                addRelationshipFilter(routeFilter).
                build();

    }

    private static class UsefulLoggingCostEvaluator extends DoubleEvaluator {

        // default implementation gives zero useful diagnostics, just throws NotFoundException

        private final String name;

        public UsefulLoggingCostEvaluator(final GraphPropertyKey costProperty) {
            super(costProperty.getText());
            this.name = costProperty.getText();
        }

        @Override
        public Double getCost(final Relationship relationship, final Direction direction) {
            try {
                return super.getCost(relationship, direction);
            }
            catch (NotFoundException exception) {
                final String msg = format("%s not found for %s dir %s type %s props %s",
                        name, relationship, direction, relationship.getType(), relationship.getAllProperties());
                logger.error(msg);
                throw new RuntimeException(msg, exception);
            }
        }
    }


}
