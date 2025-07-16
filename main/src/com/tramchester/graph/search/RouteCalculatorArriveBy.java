package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculatorArriveBy implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorArriveBy.class);

    private final RouteCostCalculator costCalculator;
    private final TramRouteCalculator routeCalculator;
    private final TramchesterConfig config;

    @Inject
    public RouteCalculatorArriveBy(RouteCostCalculator costCalculator, RouteCalculator routeCalculator, TramchesterConfig config) {
        this.costCalculator = costCalculator;
        this.routeCalculator = routeCalculator;
        this.config = config;
    }

    @Override
    public Stream<Journey> calculateRoute(ImmutableGraphTransactionNeo4J txn, Location<?> start, Location<?> destination, JourneyRequest journeyRequest, Running running) {
        try {
            final Duration costToDest = costCalculator.getAverageCostBetween(txn, start, destination, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(start, config);
            final JourneyRequest updatedRequest = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, updatedRequest));
            return routeCalculator.calculateRoute(txn, start, destination, updatedRequest, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from %s to %s for %s".formatted(start.getId(), destination.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }

    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(ImmutableGraphTransactionNeo4J txn, Location<?> start, GraphNode endOfWalk, LocationCollection destStations,
                                                   JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        try {
            final Duration costToDest = costCalculator.getAverageCostBetween(txn, start, endOfWalk, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(start, config);
            final JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtEnd(txn, start, endOfWalk, destStations, departureTime, possibleMinChanges, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from %s to node %s for %s".formatted(start.getId(), endOfWalk.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(ImmutableGraphTransactionNeo4J txn, Set<StationWalk> stationWalks, GraphNode origin, Location<?> destination,
                                                     JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        try {
            final Duration costToDest = costCalculator.getAverageCostBetween(txn, origin, destination, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(stationWalks, config);
            final JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtStart(txn, stationWalks, origin, destination, departureTime, possibleMinChanges, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from node %s to %s for %s".formatted(origin.getId(), destination.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStartAndEnd(ImmutableGraphTransactionNeo4J txn, Set<StationWalk> stationWalks, GraphNode startNode,
                                                           GraphNode endNode, LocationCollection destinationStations,
                                                           JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        try {
            final Duration costToDest = costCalculator.getAverageCostBetween(txn, startNode, endNode, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(stationWalks, config);
            final JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtStartAndEnd(txn, stationWalks, startNode, endNode, destinationStations, departureTime, possibleMinChanges, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from node %s to node %s [walks:%s] for %s".formatted(
                    startNode.getId(), endNode.getId(), stationWalks, journeyRequest), invalidDurationException);
            return Stream.empty();
        }
    }


    private JourneyRequest calcDepartTime(final JourneyRequest originalRequest, final Duration costToDest, final Duration maxInitialWait) {
        final TramTime queryTime = originalRequest.getOriginalTime();

        final TramTime departTime = queryTime.minusRounded(costToDest);

        //final int waitTime = config.getMaxInitialWait() / 2;
        final int waitTimeMinutes = Math.toIntExact(maxInitialWait.toMinutes() / 2);
        final TramTime newQueryTime = departTime.minusMinutes(waitTimeMinutes);

        return new JourneyRequest(originalRequest, newQueryTime);
    }



}
