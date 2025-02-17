package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
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
    public Stream<Journey> calculateRoute(GraphTransaction txn, Location<?> startStation, Location<?> destination, JourneyRequest journeyRequest, Running running) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, startStation, destination, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(startStation, config);
            JourneyRequest updatedRequest = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, updatedRequest));
            return routeCalculator.calculateRoute(txn, startStation, destination, updatedRequest, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from %s to %s for %s".formatted(startStation.getId(), destination.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }

    }

    @Override
    public Stream<Journey> calculateRouteWalkAtEnd(GraphTransaction txn, Location<?> start, GraphNode endOfWalk, LocationCollection destStations,
                                                   JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, start, endOfWalk, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(start, config);
            JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtEnd(txn, start, endOfWalk, destStations, departureTime, possibleMinChanges, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from %s to node %s for %s".formatted(start.getId(), endOfWalk.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode origin, Location<?> destination,
                                                     JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, origin, destination, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(stationWalks, config);
            JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtStart(txn, stationWalks, origin, destination, departureTime, possibleMinChanges, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from node %s to %s for %s".formatted(origin.getId(), destination.getId(), journeyRequest),
                    invalidDurationException);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStartAndEnd(GraphTransaction txn, Set<StationWalk> stationWalks, GraphNode startNode,
                                                           GraphNode endNode, LocationCollection destinationStations,
                                                           JourneyRequest journeyRequest, int possibleMinChanges, Running running) {
        try {
            Duration costToDest = costCalculator.getAverageCostBetween(txn, startNode, endNode, journeyRequest.getDate(), journeyRequest.getRequestedModes());
            Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(stationWalks, config);
            JourneyRequest departureTime = calcDepartTime(journeyRequest, costToDest, maxInitialWait);
            logger.info(format("Plan journey, arrive by %s so depart by %s", journeyRequest, departureTime));
            return routeCalculator.calculateRouteWalkAtStartAndEnd(txn, stationWalks, startNode, endNode, destinationStations, departureTime, possibleMinChanges, running);
        } catch (InvalidDurationException invalidDurationException) {
            logger.error("Unable to compute cost from node %s to node %s [walks:%s] for %s".formatted(
                    startNode.getId(), endNode.getId(), stationWalks, journeyRequest), invalidDurationException);
            return Stream.empty();
        }
    }


    private JourneyRequest calcDepartTime(JourneyRequest originalRequest, Duration costToDest, Duration maxInitialWait) {
        TramTime queryTime = originalRequest.getOriginalTime();

        final TramTime departTime = queryTime.minusRounded(costToDest);

        //final int waitTime = config.getMaxInitialWait() / 2;
        final int waitTimeMinutes = Math.toIntExact(maxInitialWait.toMinutes() / 2);
        TramTime newQueryTime = departTime.minusMinutes(waitTimeMinutes);

        return new JourneyRequest(originalRequest, newQueryTime);
    }



}
