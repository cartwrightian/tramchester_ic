package com.tramchester.graph.core;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.core.inMemory.GraphNodeInMemory;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.ServiceHeuristics;
import com.tramchester.graph.search.diagnostics.*;
import com.tramchester.graph.search.PreviousVisits;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public abstract class TramRouteEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    protected final ServiceHeuristics serviceHeuristics;
    protected final Set<GraphNodeId> destinationNodeIds;
    protected final ServiceReasons reasons;
    protected final PreviousVisits previousVisits;
    protected final LowestCostSeen bestResultSoFar;
    protected final boolean diagEnabled;
    protected final int maxWaitMins;
    protected final int maxInitialWaitMins;
    protected final GraphNodeId startNodeId;
    protected final EnumSet<GraphLabel> requestedLabels;
    protected final GraphTransaction txn;
    protected final boolean depthFirst;
    protected final Running running;
    protected final Set<GraphNodeId> seenTimeNode;
    protected final EnumSet<GraphLabel> destinationLabels;

    public TramRouteEvaluator(final ServiceHeuristics serviceHeuristics, final TramchesterConfig config,
                              final GraphTransaction txn, final Set<GraphNodeId> destinationNodeIds,
                              final ServiceReasons reasons, final PreviousVisits previousVisits,
                              final LowestCostSeen bestResultSoFar, final GraphNodeId startNodeId,
                              final EnumSet<TransportMode> requestedModes, Running running,
                              final EnumSet<TransportMode> destinationModes,
                              final Duration maxInitialWait) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.reasons = reasons;
        this.previousVisits = previousVisits;
        this.bestResultSoFar = bestResultSoFar;
        this.diagEnabled = reasons.getDiagnosticsEnabled();
        this.maxWaitMins = config.getMaxWait();
        this.maxInitialWaitMins = Math.toIntExact(maxInitialWait.toMinutes());
        this.startNodeId = startNodeId;
        this.requestedLabels = GraphLabel.forModes(requestedModes);
        this.txn = txn;
        this.depthFirst = config.getDepthFirst();
        this.running = running;
        this.seenTimeNode = new HashSet<>();
        this.destinationLabels = GraphLabel.forModes(destinationModes);
    }

    public GraphEvaluationAction evaluate(final GraphPath graphPath, final ImmutableJourneyState journeyState) {

        final GraphNode nextNode = graphPath.getEndNode(txn);

        // reuse these, label operations on nodes are expensive
        final EnumSet<GraphLabel> labels = nextNode.getLabels();

        final List<PropertyDTO> endNodeProps;
        if (serviceHeuristics.isDiagnostics()) {
            if (nextNode instanceof GraphNodeInMemory graphNodeInMemory) {
                endNodeProps = graphNodeInMemory.getProperties();
            } else {
                endNodeProps = Collections.emptyList();
            }
        } else {
            endNodeProps= Collections.emptyList();
        }

        GraphNodeId previousNodeId = graphPath.getPreviousNodeId(txn);
        final HowIGotHere howIGotHere = new HowIGotHere(journeyState, nextNode.getId(), previousNodeId, endNodeProps);

        // TODO WIP Spike
//        if (journeyState.alreadyVisited(nextNode, labels)) {
//            logger.debug("Returned to " + nextNode.getId() + " " + labels);
//            return Evaluation.EXCLUDE_AND_CONTINUE;
//        }

        reasons.recordVisit(howIGotHere);

        if (!running.isRunning()) {
            logger.debug("Requested to stop");
            reasons.recordReason(HeuristicsReasons.SearchStopped(howIGotHere));
            return GraphEvaluationAction.EXCLUDE_AND_PRUNE;
        }

        // NOTE: This makes a significant impact on performance, without it algo explore the same
        // path again and again for the same time in the case where it is a valid time.
        final HeuristicsReason previousResult = previousVisits.getPreviousResult(journeyState, labels, howIGotHere);
        final boolean cacheHit = (previousResult.getReasonCode() != ReasonCode.PreviousCacheMiss);
        if (cacheHit) {
            reasons.recordReason(HeuristicsReasons.Cached(previousResult, howIGotHere));
            return GraphEvaluationAction.EXCLUDE_AND_PRUNE;
        }

        reasons.recordReason(HeuristicsReasons.CacheMiss(howIGotHere));

        final HeuristicsReason heuristicsReason = doEvaluate(graphPath, journeyState, nextNode, labels, howIGotHere);

        previousVisits.cacheVisitIfUseful(heuristicsReason, nextNode, journeyState, labels);

        return heuristicsReason.getEvaluationAction();
    }

    private HeuristicsReason doEvaluate(final GraphPath thePath, final ImmutableJourneyState journeyState, final GraphNode nextNode,
                                        final EnumSet<GraphLabel> nodeLabels, final HowIGotHere howIGotHere) {

        final GraphNodeId nextNodeId = nextNode.getId();

        final Duration totalCostSoFar = journeyState.getTotalDurationSoFar();
        final int numberChanges = journeyState.getNumberChanges();

        if (destinationNodeIds.contains(nextNodeId)) { // We've Arrived
            return processArrivalAtDest(journeyState, howIGotHere, numberChanges, totalCostSoFar);
        } else if (bestResultSoFar.everArrived()) { // Not arrived for current journey, but we have seen at least one prior success
            final Duration lowestCostSeen = bestResultSoFar.getLowestDuration();
            if (Durations.greaterThan(totalCostSoFar, lowestCostSeen)) {
                // already longer that current shortest, no need to continue
                return reasons.recordReason(HeuristicsReasons.HigherCost(howIGotHere, totalCostSoFar));
            }
        }

        reasons.recordState(journeyState);

        if (diagEnabled) {
            if (nodeLabels.contains(GraphLabel.GROUPED)) {
                final IdFor<NPTGLocality> areaId = nextNode.getAreaId();
                reasons.recordReason(HeuristicReasonsOK.SeenGroup(ReasonCode.SeenGroup, howIGotHere, areaId));
            }
        }

        // WIP
        // already boarded at this location? TODO only check for certain states
        if (journeyState.justBoarded()) {
            if (journeyState.duplicatedBoardingSeen()) {
                //logger.warn("Already saw boarding at " + nextNodeId);
                return reasons.recordReason(HeuristicsReasons.AlreadyBoardedAt(howIGotHere));
            }
        }

        // no journey longer than N nodes
        if (thePath.length() > serviceHeuristics.getMaxPathLength()) {
            if (depthFirst) {
                logger.warn("Hit max path length");
            }

            return reasons.recordReason(HeuristicsReasons.PathToLong(howIGotHere));
        }

        // number of changes?
        final HeuristicsReason checkNumberChanges = serviceHeuristics.checkNumberChanges(journeyState.getNumberChanges(), howIGotHere, reasons);
        if (!checkNumberChanges.isValid()) {
            return checkNumberChanges;
        }

        // number of walks connections, usually just 2, beginning and end
        final HeuristicsReason numberWalkingConnections = serviceHeuristics.checkNumberWalkingConnections(journeyState.getNumberWalkingConnections(),
                howIGotHere, reasons);
        if (!numberWalkingConnections.isValid()) {
            return numberWalkingConnections;
        }

        // number of walks between stations aka Neighbours
        final HeuristicsReason neighbourConnections = serviceHeuristics.checkNumberNeighbourConnections(journeyState.getNumberNeighbourConnections(), howIGotHere, reasons);
        if (!neighbourConnections.isValid()) {
            return neighbourConnections;
        }

        // journey duration too long?
        final HeuristicsReason durationUnderLimit = serviceHeuristics.journeyDurationUnderLimit(totalCostSoFar, howIGotHere, reasons);
        if (!durationUnderLimit.isValid()) {
            return durationUnderLimit;
        }

        // returned to the start?
        if ((thePath.length() > 1) && nextNodeId.equals(startNodeId)) {
            return reasons.recordReason(HeuristicsReasons.ReturnedToStart(howIGotHere));
        }

        final TramTime visitingTime = journeyState.getJourneyClock();
        final int timeToWait = journeyState.hasBegunJourney() ? maxWaitMins : maxInitialWaitMins;
        // --> Minute
        // check time
        if (nodeLabels.contains(GraphLabel.MINUTE)) {
            // TODO SPIKE!
            if (!depthFirst) {
                if (seenTimeNode.contains(nextNodeId)) {
                    return reasons.recordReason(HeuristicsReasons.AlreadySeenTime(howIGotHere, nextNodeId));
                } else {
                    seenTimeNode.add(nextNodeId);
                }
            }

            final HeuristicsReason serviceReasonTripCheck = serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, nextNode, journeyState, reasons);
            if (!serviceReasonTripCheck.isValid()) {
                return serviceReasonTripCheck;
            }

            final HeuristicsReason serviceReasonTimeCheck = serviceHeuristics.checkTime(howIGotHere, nextNode, visitingTime, reasons, timeToWait);
            if (!serviceReasonTimeCheck.isValid()) {
                return serviceReasonTimeCheck;
            }
        }

        /////
        // these next are ordered by frequency / number of nodes of type

        // -->Hour
        // check hour
        if (nodeLabels.contains(GraphLabel.HOUR)) {
            final HeuristicsReason interestedInHour = serviceHeuristics.interestedInHour(howIGotHere, visitingTime, reasons, timeToWait, nodeLabels);
            if (!interestedInHour.isValid()) {
                return interestedInHour;
            }
        }

        // -->Service
        final boolean isService = nodeLabels.contains(GraphLabel.SERVICE);
        if (isService) {
            HeuristicsReason serviceDateAndTime = serviceHeuristics.checkServiceDateAndTime(nextNode, howIGotHere, reasons, visitingTime, timeToWait);
            if (!serviceDateAndTime.isValid()) {
                return serviceDateAndTime;
            }
        }

        // -->Route Station
        // is dest reachable from here and is route operating today?
        // is the station open?
        if (nodeLabels.contains(GraphLabel.ROUTE_STATION)) {

            final HeuristicsReason forMode = serviceHeuristics.checkModes(nodeLabels, requestedLabels, howIGotHere, reasons);
            if (!forMode.isValid()) {
                return forMode;
            }

            final HeuristicsReason stationOpen = serviceHeuristics.checkStationOpen(nextNode, howIGotHere, reasons);
            if (!stationOpen.isValid()) {
                // NOTE: might still reach the closed station via a walk, which is not via the RouteStation
                return stationOpen;
            }

            // TODO final change needs to match destination modes of transport
            final HeuristicsReason modesMatch = serviceHeuristics.checkModesMatchForFinalChange(journeyState.getNumberChanges(),
                    nodeLabels, destinationLabels, howIGotHere, reasons);
            if (!modesMatch.isValid()) {
                return modesMatch;
            }

            if (depthFirst) {
                // too slow for breadth first on larger graphs
                final HeuristicsReason reachDestination = serviceHeuristics.canReachDestination(nextNode, journeyState.getNumberChanges(),
                        howIGotHere, reasons, visitingTime);
                if (!reachDestination.isValid()) {
                    return reachDestination;
                }

                // Without the filtering from serviceHeuristics.canReachDestination becomes very expensive
                final HeuristicsReason serviceReason = serviceHeuristics.lowerCostIncludingInterchange(nextNode, howIGotHere, reasons);
                if (!serviceReason.isValid()) {
                    return serviceReason;
                }
            }

        }

        // TODO is this still needed, should drop through via continue anyway?
//        final GraphRelationship inboundRelationship = txn.lastFrom(thePath);
//        if (inboundRelationship != null) {
//            // for walking routes we do want to include them all even if at same time
//            if (inboundRelationship.isType(WALKS_TO_STATION)) {
//                return reasons.recordReason(HeuristicReasonsOK.IsValid(ReasonCode.WalkOk, howIGotHere));
//            }
//        }

        return reasons.recordReason(HeuristicReasonsOK.Continue(howIGotHere));
    }

    @NotNull
    private synchronized HeuristicsReason processArrivalAtDest(final ImmutableJourneyState journeyState, final HowIGotHere howIGotHere,
                                                               final int numberChanges, Duration totalCostSoFar) {

        // todo if was on diversion at any stage then change behaviour here?

        // todo set a thresh-hold on this rather than just having to be lower?
        if (bestResultSoFar.isLower(journeyState)) {
            // a better route than seen so far
            bestResultSoFar.setLowestCost(journeyState);
            return reasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere, totalCostSoFar, numberChanges));
        }

        final int lowestNumChanges = bestResultSoFar.getLowestNumChanges();
        if (numberChanges == lowestNumChanges) {
            return reasons.recordReason(HeuristicsReasons.ArrivedLater(howIGotHere, totalCostSoFar, numberChanges));
        } else if (numberChanges < lowestNumChanges) {
            // fewer hops can be a useful option
            return reasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere, totalCostSoFar, numberChanges));
        } else {
            // found a route, but longer or more hops than current shortest
            return reasons.recordReason(HeuristicsReasons.ArrivedMoreChanges(howIGotHere, numberChanges, totalCostSoFar));
        }
    }

    public boolean matchesDestination(final GraphNodeId graphNodeId) {
        return destinationNodeIds.contains(graphNodeId);
    }
}
