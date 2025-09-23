package com.tramchester.graph.search.inMemory;


import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.GraphTransactionInMemory;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.diagnostics.GraphEvaluationAction;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.DIVERSION;

public class FindPathsForJourney {

    public static final Duration NotVisitiedDuration = Duration.ofSeconds(Integer.MAX_VALUE);

    private static final Logger logger = LoggerFactory.getLogger(FindPathsForJourney.class);

    private final GraphTransactionInMemory txn;
    private final GraphNode startNode;
    private final boolean depthFirst;
    private final TramRouteEvaluator evaluator;

    public FindPathsForJourney(GraphTransaction txn, GraphNode startNode, TramchesterConfig config,  final TramRouteEvaluator evaluator) {
        this.txn = (GraphTransactionInMemory) txn;
        this.startNode = startNode;
        this.depthFirst = config.getDepthFirst();
        this.evaluator = evaluator;
    }

    public List<GraphPath> findPaths(final JourneyState journeyState) {

        final GraphPathInMemory initialPath = new GraphPathInMemory();

        final SearchState searchState = new SearchState(startNode.getId(), initialPath);

        final HasJourneyState hasJourneyState = new HasJourneyState(searchState, journeyState);

        final List<GraphPathInMemory> results = new ArrayList<>();

        while (searchState.hasNodes()) {
            final NodeSearchState nodeSearchState = searchState.getNext();
//            final GraphNodeId nextId = nodeSearchState.getNodeId();
            visitNodeOnPath(nodeSearchState, hasJourneyState, results);
        }

        return results.stream().map(item -> (GraphPath) item).toList();
    }

    private void visitNodeOnPath(final NodeSearchState nodeSearchState, final HasJourneyState graphState, final List<GraphPathInMemory> reachedDest) {
        final boolean debugEnabled = logger.isDebugEnabled();

        final GraphNodeId currentNodeId = nodeSearchState.getNodeId();
        final GraphNode currentNode = txn.getNodeById(currentNodeId);

        // TODO should include the node
        final GraphPathInMemory existingPath = nodeSearchState.getPathToHere();

        final GraphPathInMemory pathToCurrentNode = existingPath.duplicateWith(txn, currentNode);

        final ImmutableJourneyState existingState = graphState.getJourneyState();
        final GraphEvaluationAction result = evaluator.evaluate(pathToCurrentNode, existingState);

        if (result==GraphEvaluationAction.EXCLUDE_AND_PRUNE) {
            if (debugEnabled) {
                logger.debug("Exclude and prune");
            }
            return;
        }

        if (evaluator.matchesDestination(currentNodeId)) {
            if (debugEnabled) {
                logger.debug("Found destination");
            }
            reachedDest.add(pathToCurrentNode);
        }

        if (result==GraphEvaluationAction.INCLUDE_AND_PRUNE) {
            // have now added to reached dest if needed
            if (debugEnabled) {
                logger.info("Include and prune");
            }
            return;
        }

        final HasJourneyState graphStateForChildren = graphState.duplicate();
        final Stream<GraphRelationship> outgoing = expand(pathToCurrentNode, graphStateForChildren, currentNode);

        final SearchState searchState = graphStateForChildren.getSearchState();
        final Duration currentCostToNode = searchState.getCurrentCost(currentNodeId); //pair.getDuration();

        final PriorityQueue<NodeSearchState> updatedNodes = new PriorityQueue<>();
        final PriorityQueue<NodeSearchState> notVisitedYet = new PriorityQueue<>();

        outgoing.forEach(graphRelationship -> {
            final Duration relationshipCost = graphRelationship.getCost();
            final GraphNode endRelationshipNode = graphRelationship.getEndNode(txn);
            final GraphNodeId endRelationshipNodeId = endRelationshipNode.getId();
            final Duration newCost = relationshipCost.plus(currentCostToNode);

            final boolean alreadySeen = searchState.hasSeen(endRelationshipNodeId);

            final GraphPathInMemory continuePath;
            if (depthFirst) {
                continuePath = pathToCurrentNode.duplicateWith(txn, graphRelationship);
            } else {
                continuePath = pathToCurrentNode.duplicateThis();
            }

            if (alreadySeen) {
                final Duration currentDurationForEnd = searchState.getCurrentCost(endRelationshipNodeId);
                if (newCost.compareTo(currentDurationForEnd) < 0) {
                    updatedNodes.add(new NodeSearchState(endRelationshipNodeId, newCost, continuePath));
                    if (depthFirst) {
                        searchState.updateCost(endRelationshipNodeId, newCost);
                    } else {
                        searchState.updateCostAndQueue(endRelationshipNodeId, newCost, continuePath);
                    }
                } // else no update, not lower cost
            } else {
                searchState.updateCost(endRelationshipNodeId, newCost);
                notVisitedYet.add(new NodeSearchState(endRelationshipNodeId, newCost, continuePath));
            }
        });

        if (depthFirst) {
            notVisitedYet.forEach(state -> {
                visitNodeOnPath(state, graphStateForChildren, reachedDest);
            });
            updatedNodes.forEach(state -> {
                visitNodeOnPath(state, graphStateForChildren, reachedDest);
            });
        } else {
            //throw new RuntimeException("Not implemented/tested yet");
            notVisitedYet.forEach(state -> {
                searchState.addCostAndQueue(state.getNodeId(), state.duration, state.getPathToHere());
            });
            updatedNodes.forEach(state -> {
                searchState.updateCostAndQueue(state.getNodeId(), state.duration, state.getPathToHere());
            });
        }
    }

    private Stream<GraphRelationship> expand(final GraphPath path, final HasJourneyState graphState, final GraphNode currentNode) {

        final ImmutableJourneyState currentJourneyState = graphState.getJourneyState();
        final ImmutableTraversalState currentTraversalState = currentJourneyState.getTraversalState();

        if (currentNode.getId().equals(startNode.getId())) {
        //if (currentTraversalState.getStateType() == TraversalStateType.NotStartedState) {

            // point to 'real' start node -> mirroring the way the existing implementation works
            final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentJourneyState);
            final ImmutableTraversalState traversalStateForChildren = currentTraversalState.nextState(startNode.getLabels(), startNode,
                    journeyStateForChildren, Duration.ZERO);
            journeyStateForChildren.updateTraversalState(traversalStateForChildren);

            graphState.setState(journeyStateForChildren);
            
            return startNode.getRelationships(txn, GraphDirection.Outgoing, TransportRelationshipTypes.forPlanning());

        } else {
            final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentJourneyState);
            final GraphRelationship lastRelationship = path.getLastRelationship(txn);

            final Duration cost = lastRelationship.getCost();

            if (Durations.greaterThan(cost, Duration.ZERO)) {
                final Duration totalCost = currentJourneyState.getTotalDurationSoFar();
                final Duration total = totalCost.plus(cost);
                journeyStateForChildren.updateTotalCost(total);
            }

            if (lastRelationship.isType(DIVERSION)) {
                final IdFor<Station> stationId = lastRelationship.getStartStationId();
                journeyStateForChildren.beginDiversion(stationId);
            }

            final GraphNode endPathNode =  path.getEndNode(txn);
            if (!endPathNode.getId().equals(currentNode.getId())) {
                throw new RuntimeException("end node mismatch " + path + " current node " + currentNode);
            }
            final EnumSet<GraphLabel> labels = endPathNode.getLabels();

            final ImmutableTraversalState traversalStateForChildren = currentTraversalState.nextState(labels, endPathNode,
                    journeyStateForChildren, cost);

            journeyStateForChildren.updateTraversalState(traversalStateForChildren);

            graphState.setState(journeyStateForChildren);

            return traversalStateForChildren.getOutbounds();
        }

    }

    private static class HasJourneyState {
        private final SearchState searchState;
        private JourneyState journeyState;

        private HasJourneyState(SearchState searchState, JourneyState journeyState) {
            this.journeyState = journeyState;
            this.searchState = searchState;
        }

        public ImmutableJourneyState getJourneyState() {
            return journeyState;
        }

        public SearchState getSearchState() {
            return searchState;
        }

        public void setState(JourneyState replacementState) {
            this.journeyState = replacementState;
        }

        public HasJourneyState duplicate() {
            return new HasJourneyState(searchState, JourneyState.fromPrevious(journeyState));
        }
    }

    private static class SearchState {
        private final PriorityQueue<NodeSearchState> nodeQueue;
        private final Map<GraphNodeId, Duration> currentCost;

        private SearchState(GraphNodeId startNodeId, GraphPathInMemory pathToHere) {
            nodeQueue = new PriorityQueue<>();
            nodeQueue.add(new NodeSearchState(startNodeId, Duration.ZERO, pathToHere));
            currentCost = new HashMap<>();
            currentCost.put(startNodeId, Duration.ZERO);
        }

        public NodeSearchState getNext() {
            return nodeQueue.poll();
        }

        public Duration getCurrentCost(final GraphNodeId nodeId) {
            return currentCost.getOrDefault(nodeId, NotVisitiedDuration);
        }

        public void updateCost(final GraphNodeId nodeId, final Duration duration) {
            synchronized (nodeQueue) {
                currentCost.put(nodeId, duration);
            }
        }

        public boolean hasNodes() {
            return !nodeQueue.isEmpty();
        }

        public boolean hasSeen(final GraphNodeId endNodeId) {
            return currentCost.containsKey(endNodeId);
        }

        public void updateCostAndQueue(GraphNodeId graphNodeId, Duration duration, GraphPathInMemory graphPath) {
            final NodeSearchState update = new NodeSearchState(graphNodeId, duration, graphPath);

            synchronized (nodeQueue) {
                if (nodeQueue.contains(update)) {
                    nodeQueue.remove(update);
                    nodeQueue.add(update);
                } else {
                    throw new RuntimeException("Node was not in the queue " + graphNodeId);
                }
                currentCost.put(graphNodeId, duration);
            }
        }

        public void addCostAndQueue(GraphNodeId graphNodeId, Duration duration, GraphPathInMemory graphPath) {
            final NodeSearchState update = new NodeSearchState(graphNodeId, duration, graphPath);

            synchronized (nodeQueue) {
                if (nodeQueue.contains(update)) {
                    throw new RuntimeException("Already in queue " + graphNodeId);
                }
                nodeQueue.add(update);
                currentCost.put(graphNodeId, duration);
            }
        }

    }

    private static class NodeSearchState implements Comparable<NodeSearchState> {
        private final GraphNodeId nodeId;
        private final Duration duration;
        private final GraphPathInMemory pathToHere;

        private NodeSearchState(GraphNodeId nodeId, Duration duration, GraphPathInMemory pathToHere) {
            this.nodeId = nodeId;
            this.duration = duration;
            this.pathToHere = pathToHere.duplicateThis();
        }

        @Override
        public int compareTo(NodeSearchState other) {
            return this.duration.compareTo(other.duration);
        }

        public GraphNodeId getNodeId() {
            return nodeId;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            NodeSearchState nodeSearchState = (NodeSearchState) o;
            return Objects.equals(nodeId, nodeSearchState.nodeId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(nodeId);
        }

        public GraphPathInMemory getPathToHere() {
            return pathToHere;
        }

        @Override
        public String toString() {
            return "NodeSearchState{" +
                    "nodeId=" + nodeId +
                    ", duration=" + duration +
                    ", pathToHere=" + pathToHere +
                    '}';
        }
    }

    public interface GraphRelationshipFilter {
        boolean include(GraphRelationship relationship);
    }

}
