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
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.diagnostics.*;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;

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
    private final int penultimateChange;

    public ServiceHeuristics(StationRepository stationRepository, NodeContentsRepository nodeOperations,
                             JourneyConstraints journeyConstraints, TramTime actualQueryTime,
                             int currentChangesLimit) {
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;

        this.journeyConstraints = journeyConstraints;
        this.actualQueryTime = actualQueryTime;
        this.currentChangesLimit = currentChangesLimit;
        this.lowestCostsForDestRoutes = journeyConstraints.getFewestChangesCalculator();
        penultimateChange = currentChangesLimit>1 ? currentChangesLimit-1 : currentChangesLimit;
    }
    
    public HeuristicsReason checkServiceDateAndTime(final GraphNode node, final HowIGotHere howIGotHere, final ServiceReasons reasons,
                                                    final TramTime visitTime, final int maxWait) {
        reasons.incrementTotalChecked();

        final IdFor<Service> nodeServiceId = node.getServiceId();

        if (!journeyConstraints.isRunningOnDate(nodeServiceId, visitTime)) {
            return reasons.recordReason(HeuristicsReasons.DoesNotRunOnQueryDate(howIGotHere, nodeServiceId));
        }

        if (!journeyConstraints.isRunningAtTime(nodeServiceId, visitTime, maxWait)) {
            return reasons.recordReason(HeuristicsReasons.ServiceNotRunningAtTime(howIGotHere, nodeServiceId, visitTime));
        }

        return valid(ReasonCode.ServiceDateOk, howIGotHere, reasons);
    }

    public HeuristicsReason checkNumberChanges(final int currentNumChanges, final HowIGotHere howIGotHere, final ServiceReasons reasons) {
       reasons.incrementTotalChecked();

       if (currentNumChanges > currentChangesLimit) {
         return reasons.recordReason(HeuristicsReasons.TooManyChanges(howIGotHere, currentNumChanges));
       }
       return reasons.recordReason(HeuristicReasonsOK.NumChangesOK(ReasonCode.NumChangesOK, howIGotHere, currentNumChanges));
    }

    public HeuristicsReason checkNumberNeighbourConnections(final int currentNumberConnections, final HowIGotHere howIGotHere, final ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumberConnections > journeyConstraints.getMaxWalkingConnections()) {
            return reasons.recordReason(HeuristicsReasons.TooManyNeighbourConnections(howIGotHere, currentNumberConnections));
        }
        return valid(ReasonCode.NeighbourConnectionsOk, howIGotHere, reasons);
    }

    public HeuristicsReason checkNumberWalkingConnections(final int currentNumConnections, final HowIGotHere howIGotHere,
                                                          final ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumConnections > journeyConstraints.getMaxWalkingConnections()) {
            return reasons.recordReason(HeuristicsReasons.TooManyWalkingConnections(howIGotHere, currentNumConnections));
        }
        return valid(ReasonCode.NumWalkingConnectionsOk, howIGotHere, reasons);
    }

    public HeuristicsReason checkTime(final HowIGotHere howIGotHere, final GraphNode node, final TramTime currentTime,
                                      final ServiceReasons reasons, final int maxWait) {
        reasons.incrementTotalChecked();

        final TramTime nodeTime = nodeOperations.getTime(node);
        if (currentTime.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(HeuristicsReasons.AlreadyDeparted(currentTime, howIGotHere));
        }

        if (!journeyConstraints.destinationsAvailable(nodeTime)) {
            return reasons.recordReason(HeuristicsReasons.DestinationUnavailableAtTime(currentTime, howIGotHere));
        }

        // Wait to get the service?
        final TimeRange window = TimeRangePartial.of(nodeTime, Duration.ofMinutes(maxWait), Duration.ZERO);

        if (window.contains(currentTime)) {
            return reasons.recordReason(HeuristicReasonsOK.TimeOK(ReasonCode.TimeOk, howIGotHere, nodeTime));
        }

        return reasons.recordReason(HeuristicsReasons.DoesNotOperateOnTime(currentTime, howIGotHere));
    }

    public HeuristicsReason interestedInHour(final HowIGotHere howIGotHere, final TramTime currentTime,
                                          final ServiceReasons reasons, final int maxWait, final EnumSet<GraphLabel> hourLabels) {
        reasons.incrementTotalChecked();

        final int hourAtNode = GraphLabel.getHourFrom(hourLabels);

        // todo check if valid, maybe the destination is open in the following hour for example
//        if (!journeyConstraints.destinationsAvailable(hourRange)) {
//            final TramTime nodeTime = TramTime.of(hourAtNode,0);
//            return reasons.recordReason(HeuristicsReasons.DestinationUnavailableAtTime(nodeTime, howIGotHere));
//        }

        final TimeRange travelTimes = TimeRange.of(currentTime, currentTime.plusMinutes(maxWait));

        final TimeRange hourRangeToday = TimeRange.of(TramTime.of(hourAtNode, 0), TramTime.of(hourAtNode, 59));
        if (travelTimes.anyOverlap(hourRangeToday)) {
            return reasons.recordReason(HeuristicReasonsOK.HourOk(ReasonCode.HourOk, howIGotHere, currentTime, hourAtNode));
        }

        final TimeRange hourRangeTomorrow = TimeRange.of(TramTime.nextDay(hourAtNode, 0), TramTime.nextDay(hourAtNode, 59));
        if (travelTimes.anyOverlap(hourRangeTomorrow)) {
            return reasons.recordReason(HeuristicReasonsOK.HourOk(ReasonCode.HourOk, howIGotHere, currentTime, hourAtNode));
        }

        return reasons.recordReason(HeuristicsReasons.DoesNotOperateAtHour(currentTime, howIGotHere, hourAtNode));

    }

    public HeuristicsReason checkStationOpen(final GraphNode node, final HowIGotHere howIGotHere, final ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        final IdFor<RouteStation> routeStationId = node.getRouteStationId();
        final RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        final IdFor<Station> associatedStationId = routeStation.getStationId();

        if (journeyConstraints.isClosed(associatedStationId)) {
           return reasons.recordReason(HeuristicsReasons.StationClosed(howIGotHere, associatedStationId));
        }

        return valid(ReasonCode.StationOpen, howIGotHere, reasons);

    }

    public HeuristicsReason checkModes(final EnumSet<GraphLabel> modelLabels, final EnumSet<GraphLabel> requestedModeLabels,
                                       final HowIGotHere howIGotHere, final ServiceReasons reasons) {
        // todo more efficient way for intersection on EnumSets?
        if (Sets.intersection(modelLabels, requestedModeLabels).isEmpty()) {
            return reasons.recordReason(HeuristicsReasons.TransportModeWrong(howIGotHere));
        }
        return valid(ReasonCode.TransportModeOk, howIGotHere, reasons);
    }


    public HeuristicsReason checkModesMatchForFinalChange(final int currentNumberOfChanges,
                                                          final EnumSet<GraphLabel> nodeLabels, final EnumSet<GraphLabel> destinationLabels,
                                                          final HowIGotHere howIGotHere, final ServiceReasons reasons) {
        // TODO potential optimisation where only one mode is configured, in which case this check does nothing
        if (currentNumberOfChanges==penultimateChange) {
            if (Sets.intersection(nodeLabels, destinationLabels).isEmpty()) {
                return reasons.recordReason(HeuristicsReasons.StationNotReachable(howIGotHere, ReasonCode.TransportModeWrong));
            }
        }
        return valid(ReasonCode.NumChangesOK, howIGotHere, reasons);
    }

    public HeuristicsReason canReachDestination(final GraphNode endNode, final int currentNumberOfChanges, final HowIGotHere howIGotHere,
                                                final ServiceReasons reasons, final TramTime currentElapsed) {
        reasons.incrementTotalChecked();

        final IdFor<RouteStation> routeStationId = endNode.getRouteStationId();
        final RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if (routeStation==null) {
            String message = "Missing routestation " + routeStationId;
            logger.error(message);
            throw new RuntimeException(message);
        }

        final Route currentRoute = routeStation.getRoute();

        if (journeyConstraints.isUnavailable(currentRoute, currentElapsed)) {
            return reasons.recordReason(HeuristicsReasons.RouteNotToday(howIGotHere, currentRoute.getId()));
        }

        final int fewestChanges = lowestCostsForDestRoutes.getFewestChanges(currentRoute);

        if (fewestChanges > currentChangesLimit) {
            return reasons.recordReason(HeuristicsReasons.StationNotReachable(howIGotHere, ReasonCode.TooManyRouteChangesRequired));
        }

        if ((fewestChanges+currentNumberOfChanges) > currentChangesLimit) {
            return reasons.recordReason(HeuristicsReasons.StationNotReachable(howIGotHere, ReasonCode.TooManyInterchangesRequired));
        }

        return valid(ReasonCode.Reachable, howIGotHere, reasons);
    }

    public HeuristicsReason lowerCostIncludingInterchange(final GraphNode nextNode, final HowIGotHere howIGotHere, final ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        final IdFor<RouteStation> routeStationId = nextNode.getRouteStationId();
        final RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if  (lowestCostsForDestRoutes.getFewestChanges(routeStation.getRoute())==0) {
            // on same route our destination
            return valid(ReasonCode.ReachableSameRoute, howIGotHere, reasons);
        }
        // otherwise, a change to a different route is needed

        return valid(ReasonCode.Reachable, howIGotHere, reasons);
    }

    public HeuristicsReason journeyDurationUnderLimit(final Duration totalDuration, final HowIGotHere howIGotHere, final ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (Durations.greaterThan(totalDuration, journeyConstraints.getMaxJourneyDuration())) {
            return reasons.recordReason(HeuristicsReasons.TookTooLong(actualQueryTime.plusRounded(totalDuration), howIGotHere));
        }
        return valid(ReasonCode.DurationOk, howIGotHere, reasons);
    }

    private HeuristicsReason valid(final ReasonCode code, final HowIGotHere howIGotHere, final ServiceReasons reasons) {
        return reasons.recordReason(HeuristicReasonsOK.IsValid(code, howIGotHere));
    }

    public int getMaxPathLength() {
        return journeyConstraints.getMaxPathLength();
    }


    public HeuristicsReason checkNotBeenOnTripBefore(final HowIGotHere howIGotHere, final GraphNode minuteNode, final ImmutableJourneyState journeyState,
                                                     final ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        final IdFor<Trip> tripId = minuteNode.getTripId();
        if (journeyState.alreadyDeparted(tripId)) {
            return reasons.recordReason(HeuristicsReasons.SameTrip(tripId, howIGotHere));
        }
        return valid(ReasonCode.Continue, howIGotHere, reasons);
    }

}
