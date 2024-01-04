package com.tramchester.graph.search;

import com.google.common.collect.Sets;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.diagnostics.*;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

public class ServiceHeuristics {

    private static final Logger logger;

    static {
        logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    }

    private final JourneyConstraints journeyConstraints;
    private final TramTime actualQueryTime;
    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeOperations;
    private final int currentChangesLimit;
    private final LowestCostsForDestRoutes lowestCostsForDestRoutes;
//    private final RouteInterchangeRepository routeInterchanges;

    public ServiceHeuristics(StationRepository stationRepository, NodeContentsRepository nodeOperations,
                             JourneyConstraints journeyConstraints, TramTime actualQueryTime,
                             int currentChangesLimit) {
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;

        this.journeyConstraints = journeyConstraints;
        this.actualQueryTime = actualQueryTime;
        this.currentChangesLimit = currentChangesLimit;
        this.lowestCostsForDestRoutes = journeyConstraints.getFewestChangesCalculator();
    }
    
    public HeuristicsReason checkServiceDateAndTime(GraphNode node, HowIGotHere howIGotHere, ServiceReasons reasons,
                                                    TramTime visitTime, int maxWait) {
        reasons.incrementTotalChecked();

        final IdFor<Service> nodeServiceId = nodeOperations.getServiceId(node);

        if (!journeyConstraints.isRunningOnDate(nodeServiceId, visitTime)) {
            return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, nodeServiceId));

        }

        if (!journeyConstraints.isRunningAtTime(nodeServiceId, visitTime, maxWait)) {
            return reasons.recordReason(ServiceReason.ServiceNotRunningAtTime(howIGotHere, nodeServiceId, visitTime));
        }

        return valid(ReasonCode.ServiceDateOk, howIGotHere, reasons);
    }

    public HeuristicsReason checkNumberChanges(int currentNumChanges, HowIGotHere howIGotHere, ServiceReasons reasons) {
       reasons.incrementTotalChecked();

       if (currentNumChanges > currentChangesLimit) {
         return reasons.recordReason(ServiceReason.TooManyChanges(howIGotHere, currentNumChanges));
       }
       return valid(ReasonCode.NumChangesOK, howIGotHere, reasons);
    }

    public HeuristicsReason checkNumberNeighbourConnections(int currentNumberConnections, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumberConnections > journeyConstraints.getMaxWalkingConnections()) {
            return reasons.recordReason(ServiceReason.TooManyNeighbourConnections(howIGotHere, currentNumberConnections));
        }
        return valid(ReasonCode.NeighbourConnectionsOk, howIGotHere, reasons);
    }

    public HeuristicsReason checkNumberWalkingConnections(int currentNumConnections, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumConnections > journeyConstraints.getMaxWalkingConnections()) {
            return reasons.recordReason(ServiceReason.TooManyWalkingConnections(howIGotHere, currentNumConnections));
        }
        return valid(ReasonCode.NumWalkingConnectionsOk, howIGotHere, reasons);
    }

    public HeuristicsReason checkTime(HowIGotHere howIGotHere, GraphNode node, TramTime currentTime, ServiceReasons reasons, int maxWait) {
        reasons.incrementTotalChecked();

        final TramTime nodeTime = nodeOperations.getTime(node);
        if (currentTime.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(ServiceReason.AlreadyDeparted(currentTime, howIGotHere));
        }

        // Wait to get the service?
        final TimeRange window = TimeRange.of(nodeTime, Duration.ofMinutes(maxWait), Duration.ZERO);

        if (window.contains(currentTime)) {
            return valid(ReasonCode.TimeOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(currentTime, howIGotHere));
    }

    public HeuristicsReason interestedInHour(HowIGotHere howIGotHere, TramTime journeyClockTime,
                                          ServiceReasons reasons, int maxWait, EnumSet<GraphLabel> labels) {
        reasons.incrementTotalChecked();

        final int queryTimeHour = journeyClockTime.getHourOfDay();

        //noinspection SuspiciousMethodCalls
        if (labels.contains(GraphLabel.getHourLabel(queryTimeHour))) {
            // quick win
            return valid(ReasonCode.HourOk, howIGotHere, reasons);
        }

        final int hourAtNode = GraphLabel.getHourFrom(labels);

        // TODO Need better way to handle this
        final TramTime beginWindow = hourAtNode==0 ? TramTime.nextDay(0,0) : TramTime.of(hourAtNode, 0);

        final TimeRange windowForWait = TimeRange.of(beginWindow, Duration.ofMinutes(maxWait), Duration.ZERO);

        if (windowForWait.contains(journeyClockTime)) {
            return valid(ReasonCode.HourOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, howIGotHere));
    }

    public HeuristicsReason checkStationOpen(GraphNode node, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        final IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(node);
        final RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        final Station associatedStation = routeStation.getStation();

        if (journeyConstraints.isClosed(associatedStation)) {
           return reasons.recordReason(ServiceReason.StationClosed(howIGotHere, associatedStation.getId()));
        }

        return valid(ReasonCode.StationOpen, howIGotHere, reasons);

    }

    public HeuristicsReason checkModes(final Set<GraphLabel> modelLabels, final Set<GraphLabel> requestedModeLabels,
                                       HowIGotHere howIGotHere, ServiceReasons reasons) {

        if (Sets.intersection(modelLabels, requestedModeLabels).isEmpty()) {
            //IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(node);
            return reasons.recordReason(ServiceReason.TransportModeWrong(howIGotHere));
        }
        return valid(ReasonCode.TransportModeOk, howIGotHere, reasons);
    }

    public HeuristicsReason canReachDestination(GraphNode endNode, int currentNumberOfChanges, HowIGotHere howIGotHere,
                                             ServiceReasons reasons, TramTime currentElapsed) {
        reasons.incrementTotalChecked();

        final IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(endNode);
        final RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if (routeStation==null) {
            String message = "Missing routestation " + routeStationId;
            logger.error(message);
            throw new RuntimeException(message);
        }

        final Route currentRoute = routeStation.getRoute();

        if (journeyConstraints.isUnavailable(currentRoute, currentElapsed)) {
            return reasons.recordReason(ServiceReason.RouteNotToday(howIGotHere, currentRoute.getId()));
        }

        final int fewestChanges = lowestCostsForDestRoutes.getFewestChanges(currentRoute);

        if (fewestChanges > currentChangesLimit) {
            return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere, ReasonCode.TooManyRouteChangesRequired));
        }

        if ((fewestChanges+currentNumberOfChanges) > currentChangesLimit) {
            return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere, ReasonCode.TooManyInterchangesRequired));
        }

        return valid(ReasonCode.Reachable, howIGotHere, reasons);
    }

    public HeuristicsReason lowerCostIncludingInterchange(GraphNode nextNode, Duration totalCostSoFar, LowestCostSeen bestSoFar,
                                                       HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        final IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(nextNode);
        final RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if  (lowestCostsForDestRoutes.getFewestChanges(routeStation.getRoute())==0) {
            // on same route our destination
            return valid(ReasonCode.ReachableSameRoute, howIGotHere, reasons);
        }
        // otherwise, a change to a different route is needed

        // little diff to tram performance, too costly to (pre-)compute for buses
//        final boolean hasPathToInterchange = routeInterchanges.hasPathToInterchange(routeStation);
//
//        if (!hasPathToInterchange) {
//            // change required from current route, but no interchange is available for this station/route combination
//            return reasons.recordReason(ServiceReason.InterchangeNotReachable(howIGotHere));
//        }

        return valid(ReasonCode.Reachable, howIGotHere, reasons);
    }

    public HeuristicsReason journeyDurationUnderLimit(final Duration totalDuration, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (Durations.greaterThan(totalDuration, journeyConstraints.getMaxJourneyDuration())) {
            return reasons.recordReason(ServiceReason.TookTooLong(actualQueryTime.plus(totalDuration), howIGotHere));
        }
        return valid(ReasonCode.DurationOk, howIGotHere, reasons);
    }

    private HeuristicsReason valid(ReasonCode code, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        return reasons.recordReason(ServiceReason.IsValid(code, howIGotHere));
    }

    public int getMaxPathLength() {
        return journeyConstraints.getMaxPathLength();
    }

    public HeuristicsReason notAlreadySeen(ImmutableJourneyState journeyState, GraphNode nextNode, final HowIGotHere howIGotHere,
                                        ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        //return getStationIdFrom(node.getNode());
        final IdFor<Station> stationId = nextNode.getStationId();
        if (journeyState.hasVisited(stationId)) {
            return reasons.recordReason(ServiceReason.AlreadySeenStation(stationId, howIGotHere));
        }
        return valid(ReasonCode.Continue, howIGotHere, reasons);
    }


    public HeuristicsReason checkNotBeenOnTripBefore(HowIGotHere howIGotHere, GraphNode minuteNode, ImmutableJourneyState journeyState, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        final IdFor<Trip> tripId = nodeOperations.getTripId(minuteNode);
        if (journeyState.alreadyDeparted(tripId)) {
            return reasons.recordReason(ServiceReason.SameTrip(tripId, howIGotHere));
        }
        return valid(ReasonCode.Continue, howIGotHere, reasons);
    }
}
