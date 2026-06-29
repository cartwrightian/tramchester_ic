package com.tramchester.graph.search.inMemory;


import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.GraphTransactionInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;
import com.tramchester.graph.reference.GraphLabels;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.diagnostics.GraphEvaluationAction;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.DIVERSION;

public class FindPathsForJourney {

    public static final TramDuration NotVisitedDuration = TramDuration.MAX_VALUE;

    private static final Logger logger = LoggerFactory.getLogger(FindPathsForJourney.class);

    private final GraphTransactionInMemory txn;
    private final GraphNode startNode;
    private final boolean depthFirst;
    private final TramRouteEvaluator evaluator;
    private final TraversalStateFactory traversalStateFactory;
    private final long numberJourneys;

    public FindPathsForJourney(final GraphTransaction txn, final GraphNode startNode, final TramchesterConfig config,
                               final TramRouteEvaluator evaluator, final TraversalStateFactory traversalStateFactory, long numberJourneys) {
        this.txn = (GraphTransactionInMemory) txn;
        this.startNode = startNode;
        this.depthFirst = config.getDepthFirst();
        this.evaluator = evaluator;
        this.traversalStateFactory = traversalStateFactory;
        this.numberJourneys = numberJourneys;
    }

    public Stream<GraphPath> findPaths(final TramTime actualQueryTime, final Running running) {

        final NotStartedState initialTraversalState = new NotStartedState(traversalStateFactory, startNode.getId(), txn);

        final JourneyState journeyState = new JourneyState(actualQueryTime, initialTraversalState);

        final GraphPathInMemory initialPath = new GraphPathInMemory();

        final SearchStateKey stateKey = SearchStateKey.create(initialPath, startNode.getId());
        final PathSearchState searchState = new PathSearchState(stateKey, initialPath, numberJourneys);
        searchState.setJourneyState(stateKey, journeyState);

        // TODO ideally want yield/stream here
        while (searchState.continueSearch() && running.isRunning()) {
            final NodeSearchState nodeSearchState = searchState.getNext();
            visitNodeOnPath(nodeSearchState, searchState);
        }

        final List<GraphPathInMemory> results = searchState.getFoundPaths();
        searchState.clear();

        // downcast
        return results.stream().map(item -> item);
    }

    private void visitNodeOnPath(final NodeSearchState nodeSearchState, final PathSearchState searchState) {

        final boolean debugEnabled = logger.isDebugEnabled();

        final SearchStateKey stateKey = nodeSearchState.getStateKey();
        final GraphNode currentNode = txn.getNodeById(stateKey.getNodeId());

        final GraphPathInMemory existingPath = nodeSearchState.getPathToHere();
        final GraphPathInMemory pathToCurrentNode = existingPath.duplicateWith(txn, currentNode);

        final JourneyState existingState = searchState.getJourneyStateFor(stateKey);

        final JourneyState graphStateForChildren = getNextState(existingPath, existingState, currentNode);

        final GraphEvaluationAction result = evaluator.evaluate(pathToCurrentNode, existingState);

        if (result==GraphEvaluationAction.EXCLUDE_AND_PRUNE) {
            if (debugEnabled) {
                logger.debug("Exclude and prune");
            }
            return;
        }

        if (evaluator.matchesDestination(stateKey.getNodeId())) {
            if (debugEnabled) {
                logger.debug("Found destination");
            }
            searchState.addFoundPath(pathToCurrentNode);
        }

        if (result==GraphEvaluationAction.INCLUDE_AND_PRUNE) {
            // have now added to reached dest if needed
            if (debugEnabled) {
                logger.info("Include and prune");
            }
            return;
        }

        final Stream<GraphRelationship> outgoing = expand(graphStateForChildren, currentNode);

        final List<NodeSearchState> updatedNodes = new LinkedList<>();
        final List<NodeSearchState> notVisitedYet = new LinkedList<>();

        final TramDuration currentCostToNode = searchState.getCurrentCost(stateKey);
        outgoing.forEach(graphRelationship -> {
            final GraphNodeId endNodeId = graphRelationship.getEndNodeId(txn);

            final GraphPathInMemory continuePath;
            if (depthFirst) {
                continuePath = pathToCurrentNode.duplicateWith(txn, graphRelationship);
            } else {
                continuePath = pathToCurrentNode.duplicate();
            }

            final TramDuration relationshipCost = graphRelationship.getCost();
            final TramDuration newCost = relationshipCost.plus(currentCostToNode);

            final SearchStateKey endStateKey = SearchStateKey.create(pathToCurrentNode, endNodeId);
            final boolean alreadySeen = searchState.hasSeen(endStateKey);

            // prioritise those Towards Dest
            final boolean towardsDest = evaluator.matchesDestination(endNodeId);
            final NodeSearchState nextSearchNodeState = NodeSearchState.createNodeSearchState(endStateKey, newCost, continuePath, towardsDest);

            if (alreadySeen) {
                final TramDuration currentDurationForEnd = searchState.getCurrentCost(endStateKey);
                // TODO is this correct?
                if (newCost.compareTo(currentDurationForEnd) != 0) {
                    updatedNodes.add(nextSearchNodeState);
                }
            } else { // not seen before
                searchState.updateCost(endStateKey, newCost);
                notVisitedYet.add(nextSearchNodeState);
            }
        });

        notVisitedYet.forEach(toVisit -> {
            searchState.addCostAndQueue(toVisit);
            searchState.setJourneyState(toVisit.getStateKey(), graphStateForChildren);
        });

        updatedNodes.forEach(toUpdate -> {
            final SearchStateKey searchStateKey = toUpdate.getStateKey();
            if (depthFirst) {
                searchState.updateCost(searchStateKey, toUpdate.getDuration());
            } else {
                searchState.updateCostAndUpdateQueue(toUpdate);
            }
            searchState.setJourneyState(searchStateKey, graphStateForChildren);
        });
    }

    JourneyState getNextState(final GraphPath pathToHere, final JourneyState currentJourneyState, final GraphNode currentNode) {

        final ImmutableTraversalState currentTraversalState = currentJourneyState.getTraversalState();

        final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentJourneyState);

        if (currentNode.getId().equals(startNode.getId())) {
            // point to 'real' start node -> mirroring the way the existing implementation works
            final ImmutableTraversalState nextTraversalState = currentTraversalState.nextState(startNode.getLabels(), startNode,
                    journeyStateForChildren, TramDuration.ZERO);
            journeyStateForChildren.updateTraversalState(nextTraversalState);

        } else {
            final GraphRelationship lastRelationship = pathToHere.getLastRelationship(txn);
            final TramDuration cost = lastRelationship.getCost();

            if (Durations.greaterThan(cost, TramDuration.ZERO)) {
                final TramDuration totalCost = currentJourneyState.getTotalDurationSoFar();
                final TramDuration total = totalCost.plus(cost);
                journeyStateForChildren.updateTotalCost(total);
            }

            if (lastRelationship.isType(DIVERSION)) {
                final IdFor<Station> stationId = lastRelationship.getStartStationId(txn);
                journeyStateForChildren.beginDiversion(stationId);
            }

            // sanity check
            if (!lastRelationship.getEndNodeId(txn).equals(currentNode.getId())) {
                throw new RuntimeException("end node mismatch " + pathToHere + " current node " + currentNode);
            }

            final GraphLabels labels = currentNode.getLabels();
            final ImmutableTraversalState traversalStateForChildren = currentTraversalState.nextState(labels, currentNode,
                    journeyStateForChildren, cost);

            journeyStateForChildren.updateTraversalState(traversalStateForChildren);
        }

        return journeyStateForChildren;

    }

    private Stream<GraphRelationship> expand(final JourneyState currentState, final GraphNode currentNode) {

        if (currentNode.getId().equals(startNode.getId())) {
            return startNode.getRelationships(txn, GraphDirection.Outgoing, TransportRelationshipTypes.forPlanning());
        } else {
            final ImmutableTraversalState currentTraversalState = currentState.getTraversalState();
            return currentTraversalState.getOutbounds();
        }
    }


    public interface GraphRelationshipFilter {
        boolean include(GraphRelationship relationship);
    }

}
