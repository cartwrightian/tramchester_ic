package com.tramchester.graph.search.inMemory;


import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.GraphTransactionInMemory;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.diagnostics.GraphEvaluationAction;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
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
    private final TraversalStateFactory traversalStateFactory;

    public FindPathsForJourney(GraphTransaction txn, GraphNode startNode, TramchesterConfig config,
                               final TramRouteEvaluator evaluator, TraversalStateFactory traversalStateFactory) {
        this.txn = (GraphTransactionInMemory) txn;
        this.startNode = startNode;
        this.depthFirst = config.getDepthFirst();
        this.evaluator = evaluator;
        this.traversalStateFactory = traversalStateFactory;
    }

    public List<GraphPath> findPaths(final TramTime actualQueryTime) {

        final NotStartedState initialTraversalState = new NotStartedState(traversalStateFactory, startNode.getId(), txn);

        final JourneyState journeyState = new JourneyState(actualQueryTime, initialTraversalState);

        final GraphPathInMemory initialPath = new GraphPathInMemory();

        final SearchState searchState = new SearchState(startNode.getId(), initialPath);
        searchState.setJourneyState(startNode.getId(), journeyState);

        while (searchState.hasNodes()) {
            final NodeSearchState nodeSearchState = searchState.getNext();
            visitNodeOnPath(nodeSearchState, searchState);
        }

        final List<GraphPathInMemory> results = searchState.getFoundPaths();
        return results.stream().map(item -> (GraphPath) item).toList();
    }

    private void visitNodeOnPath(final NodeSearchState nodeSearchState,
                                 final SearchState searchState) {

        final boolean debugEnabled = logger.isDebugEnabled();

        final GraphNodeId currentNodeId = nodeSearchState.getNodeId();
        final GraphNode currentNode = txn.getNodeById(currentNodeId);

        // TODO should include the node
        final GraphPathInMemory existingPath = nodeSearchState.getPathToHere();
        final GraphPathInMemory pathToCurrentNode = existingPath.duplicateWith(txn, currentNode);

        final JourneyState existingState = searchState.getJourneyStateFor(currentNodeId);

        final JourneyState graphStateForChildren = getNextState(existingPath, existingState, currentNode);

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

        final Duration currentCostToNode = searchState.getCurrentCost(currentNodeId);

        final PriorityQueue<NodeSearchState> updatedNodes = new PriorityQueue<>();
        final PriorityQueue<NodeSearchState> notVisitedYet = new PriorityQueue<>();

        if (existingState.getTraversalStateType().equals(TraversalStateType.NotStartedState)) {
            final NodeSearchState state = new NodeSearchState(currentNodeId, Duration.ZERO, existingPath);
            searchState.addCostAndQueue(state.getNodeId(), state.duration, state.getPathToHere());
            searchState.setJourneyState(currentNodeId, graphStateForChildren);
            return;
        }

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

//        if (depthFirst) {
//            notVisitedYet.forEach(state -> {
//                visitNodeOnPath(state, graphStateForChildren, searchState);
//            });
//            updatedNodes.forEach(state -> {
//                visitNodeOnPath(state, graphStateForChildren, searchState);
//            });
//        } else {
            notVisitedYet.forEach(state -> {
                searchState.addCostAndQueue(state.getNodeId(), state.duration, state.getPathToHere());
                searchState.setJourneyState(state.getNodeId(), graphStateForChildren);
            });
            updatedNodes.forEach(state -> {
                searchState.updateCostAndQueue(state.getNodeId(), state.duration, state.getPathToHere());
                searchState.setJourneyState(state.getNodeId(), graphStateForChildren);
            });
//        }
    }

    JourneyState getNextState(final GraphPath pathToHere, final JourneyState currentJourneyState, final GraphNode currentNode) {

        final ImmutableTraversalState currentTraversalState = currentJourneyState.getTraversalState();

        final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentJourneyState);

        if (currentNode.getId().equals(startNode.getId())) {
            // point to 'real' start node -> mirroring the way the existing implementation works
            final ImmutableTraversalState nextTraversalState = currentTraversalState.nextState(startNode.getLabels(), startNode,
                    journeyStateForChildren, Duration.ZERO);
            journeyStateForChildren.updateTraversalState(nextTraversalState);

        } else {
            //final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentJourneyState);
            final GraphRelationship lastRelationship = pathToHere.getLastRelationship(txn);

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

            // sanity check
            //final GraphRelationship lastRelationship =  pathToHere.getLastRelationship(txn);
            if (!lastRelationship.getEndNodeId(txn).equals(currentNode.getId())) {
                throw new RuntimeException("end node mismatch " + pathToHere + " current node " + currentNode);
            }
            //GraphNode endPathNode = lastRelationship.getEndNode(txn);

            final EnumSet<GraphLabel> labels = currentNode.getLabels();
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
            //final ImmutableJourneyState currentJourneyState = currentState.getJourneyState();
            final ImmutableTraversalState currentTraversalState = currentState.getTraversalState();

            return currentTraversalState.getOutbounds();
        }
    }

    private static class SearchState {
        private final PriorityQueue<NodeSearchState> nodeQueue;
        private final Map<GraphNodeId, Duration> currentCost;
        private final Map<GraphNodeId, JourneyState> journeyStates;

        final List<GraphPathInMemory> foundPaths;

        private SearchState(GraphNodeId startNodeId, GraphPathInMemory pathToHere) {
            nodeQueue = new PriorityQueue<>();
            nodeQueue.add(new NodeSearchState(startNodeId, Duration.ZERO, pathToHere));
            currentCost = new HashMap<>();
            currentCost.put(startNodeId, Duration.ZERO);
            foundPaths = new ArrayList<>();
            journeyStates = new HashMap<>();
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

        public void updateCostAndQueue(final GraphNodeId graphNodeId, final Duration duration, final GraphPathInMemory graphPath) {
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

            addCostAndQueue(update);
        }

        private void addCostAndQueue(NodeSearchState update) {
            synchronized (nodeQueue) {
                if (nodeQueue.contains(update)) {
                    throw new RuntimeException("Already in queue " + update.getNodeId());
                }
                nodeQueue.add(update);
                currentCost.put(update.getNodeId(), update.duration);
            }
        }

        public List<GraphPathInMemory> getFoundPaths() {
            return foundPaths;
        }

        public void addFoundPath(final GraphPathInMemory path) {
            synchronized (foundPaths) {
                foundPaths.add(path);
            }
        }

        @Override
        public String toString() {
            return "SearchState{" +
                    "nodeQueue=" + nodeQueue +
                    ", currentCost=" + currentCost +
                    ", foundPaths=" + foundPaths +
                    '}';
        }

        public void setJourneyState(final GraphNodeId id, final JourneyState journeyState) {
            journeyStates.put(id, journeyState);
        }

        public JourneyState getJourneyStateFor(final GraphNodeId nodeId) {
            if (!journeyStates.containsKey(nodeId)) {
                throw new RuntimeException("No journey state for " + nodeId);
            }
            return journeyStates.get(nodeId);
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
