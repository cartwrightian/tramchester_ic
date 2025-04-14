package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.diagnostics.*;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    private final ServiceHeuristics serviceHeuristics;
    private final NodeContentsRepository nodeContentsRepository;

    private final Set<GraphNodeId> destinationNodeIds;
    private final ServiceReasons reasons;
    private final PreviousVisits previousVisits;
    private final LowestCostSeen bestResultSoFar;
    private final boolean diagEnabled;

    private final int maxWaitMins;
    private final int maxInitialWaitMins;
    private final GraphNodeId startNodeId;
    private final EnumSet<GraphLabel> requestedLabels;
    private final GraphTransaction txn;
    private final boolean depthFirst;
    private final Running running;

    private final Set<GraphNodeId> seenTimeNode;
    private final EnumSet<GraphLabel> destinationLabels;

    public TramRouteEvaluator(final RouteCalculatorSupport.PathRequest pathRequest, final Set<GraphNodeId> destinationNodeIds,
                              final NodeContentsRepository nodeContentsRepository, final ServiceReasons reasons,
                              final PreviousVisits previousVisits, final LowestCostSeen bestResultSoFar, final TramchesterConfig config,
                              final GraphNodeId startNodeId,
                              final GraphTransaction txn, Running running) {
        this(pathRequest.getServiceHeuristics(), destinationNodeIds, nodeContentsRepository, reasons, previousVisits,
                bestResultSoFar, config, startNodeId, pathRequest.getRequestedModes(),
                pathRequest.getDesintationModes(),
                pathRequest.getMaxInitialWait(), txn, running);
    }

    public TramRouteEvaluator(final ServiceHeuristics serviceHeuristics, final Set<GraphNodeId> destinationNodeIds,
                              final NodeContentsRepository nodeContentsRepository, final ServiceReasons reasons,
                              final PreviousVisits previousVisits, final LowestCostSeen bestResultSoFar, final TramchesterConfig config,
                              final GraphNodeId startNodeId, final EnumSet<TransportMode> requestedModes,
                              final EnumSet<TransportMode> destinationModes,
                              final Duration maxInitialWait, final GraphTransaction txn, Running running) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.nodeContentsRepository = nodeContentsRepository;
        this.reasons = reasons;
        this.previousVisits = previousVisits;
        this.bestResultSoFar = bestResultSoFar;

        this.startNodeId = startNodeId;
        this.requestedLabels = GraphLabel.forModes(requestedModes);
        this.destinationLabels = GraphLabel.forModes(destinationModes);
        this.maxInitialWaitMins = Math.toIntExact(maxInitialWait.toMinutes());
        this.txn = txn;

        maxWaitMins = config.getMaxWait();
        depthFirst = config.getDepthFirst();
        this.diagEnabled = reasons.getDiagnosticsEnabled();

        seenTimeNode = new HashSet<>();

        this.running = running;
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(final Path path, final BranchState<JourneyState> state) {

        final ImmutableJourneyState journeyState = state.getState();

        final GraphNode nextNode = txn.fromEnd(path);
        final GraphRelationship last = txn.lastFrom(path);

        // reuse these, label operations on nodes are expensive
        final EnumSet<GraphLabel> labels = nextNode.getLabels();

        final HowIGotHere howIGotHere = new HowIGotHere(journeyState, nextNode.getId(), getPreviousNodeSafe(last));

        // TODO WIP Spike
//        if (journeyState.alreadyVisited(nextNode, labels)) {
//            logger.debug("Returned to " + nextNode.getId() + " " + labels);
//            return Evaluation.EXCLUDE_AND_CONTINUE;
//        }

        reasons.recordVisit(howIGotHere);

        if (!running.isRunning()) {
            logger.debug("Requested to stop");
            reasons.recordReason(HeuristicsReasons.SearchStopped(howIGotHere));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // NOTE: This makes a significant impact on performance, without it algo explore the same
        // path again and again for the same time in the case where it is a valid time.
        final HeuristicsReason previousResult = previousVisits.getPreviousResult(journeyState, labels, howIGotHere);
        final boolean cacheHit = (previousResult.getReasonCode() != ReasonCode.PreviousCacheMiss);
        if (cacheHit) {
            reasons.recordReason(HeuristicsReasons.Cached(previousResult, howIGotHere));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        reasons.recordReason(HeuristicsReasons.CacheMiss(howIGotHere));

        final HeuristicsReason heuristicsReason = doEvaluate(path, journeyState, nextNode, labels, howIGotHere);
        final Evaluation result = heuristicsReason.getEvaluationAction();

        previousVisits.cacheVisitIfUseful(heuristicsReason, nextNode, journeyState, labels);

        return result;
    }


    private GraphNodeId getPreviousNodeSafe(final GraphRelationship graphRelationship) {
        if (graphRelationship==null) {
            return null;
        }
        return graphRelationship.getStartNodeId(txn);
    }

    private HeuristicsReason doEvaluate(final Path thePath, final ImmutableJourneyState journeyState, final GraphNode nextNode,
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

        // no journey longer than N nodes
        // TODO check length based on current transport mode??
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
        //final Relationship inboundRelationship = thePath.lastRelationship();
        final GraphRelationship inboundRelationship = txn.lastFrom(thePath);
        if (inboundRelationship != null) {
            // for walking routes we do want to include them all even if at same time
            if (inboundRelationship.isType(WALKS_TO_STATION)) {
                return reasons.recordReason(HeuristicReasonsOK.IsValid(ReasonCode.WalkOk, howIGotHere));
            }
        }

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

}
