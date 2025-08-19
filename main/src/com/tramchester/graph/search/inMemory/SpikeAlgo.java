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
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.DIVERSION;

public class SpikeAlgo {

    private static final Duration notVisitiedDuration = Duration.ofSeconds(Integer.MAX_VALUE);

    private static final Logger logger = LoggerFactory.getLogger(SpikeAlgo.class);

    private final GraphTransactionInMemory txn;
    private final GraphNode startNode;
    private final TramchesterConfig config;
    private final TramRouteEvaluator evaluator;

    public SpikeAlgo(GraphTransaction txn, GraphNode startNode, TramchesterConfig config,
                     TramRouteEvaluator evaluator) {
        this.txn = (GraphTransactionInMemory) txn;
        this.startNode = startNode;
        this.config = config;
        this.evaluator = evaluator;
    }

    public List<GraphPath> findRoute(final JourneyState journeyState) {

        GraphPathInMemory initialPath = new GraphPathInMemory();

        final SearchState searchState = new SearchState(startNode.getId(), initialPath);

        final HasJourneyState hasJourneyState = new HasJourneyState(searchState, journeyState);

        final List<GraphPathInMemory> results = new ArrayList<>();

        while (searchState.hasNodes()) {
            final NodeSearchState nodeSearchState = searchState.getNext();
            final GraphNodeId nextId = nodeSearchState.getNodeId();
            visitNode(nextId, hasJourneyState, nodeSearchState.getPathToHere(), results);
        }

        return results.stream().map(item -> (GraphPath) item).toList();
    }

    private void visitNode(final GraphNodeId nodeId, final HasJourneyState graphState, GraphPathInMemory existingPath,
                           List<GraphPathInMemory> reachedDest) {

        final GraphNode currentNode = txn.getNodeById(nodeId);

        final GraphPathInMemory pathToCurrentNode = existingPath.duplicateWith(txn, currentNode);

        ImmutableJourneyState existingState = graphState.getJourneyState();

        final GraphEvaluationAction result = evaluator.evaluate(pathToCurrentNode, existingState);

        if (result==GraphEvaluationAction.EXCLUDE_AND_PRUNE) {
            logger.info("Exclude and prune");
            return;
        }

        if (evaluator.matchesDestination(currentNode)) {
            logger.info("Found destination");
            reachedDest.add(pathToCurrentNode);
        }

        if (result==GraphEvaluationAction.INCLUDE_AND_PRUNE) {
            // have now added to reached dest if needed
            logger.info("Include and prune");
            return;
        }

        final HasJourneyState graphStateForChildren = graphState.duplicate();
        Stream<GraphRelationship> outgoing = expand(pathToCurrentNode, graphStateForChildren);

        SearchState searchState = graphStateForChildren.getSearchState();
        final Duration currentCostToNode = searchState.getCurrentCost(nodeId); //pair.getDuration();

        outgoing.forEach(graphRelationship -> {
            final Duration relationshipCost = graphRelationship.getCost();
            final GraphNode endRelationshipNode = graphRelationship.getEndNode(txn);
            final GraphNodeId endRelationshipNodeId = endRelationshipNode.getId();
            final Duration newCost = relationshipCost.plus(currentCostToNode);

            boolean updated = false;
            final GraphPathInMemory continuePath = pathToCurrentNode.duplicateWith(txn, graphRelationship);
            if (searchState.hasSeen(endRelationshipNodeId)) {
                final Duration currentDurationForEnd = searchState.getCurrentCost(endRelationshipNodeId);
                if (newCost.compareTo(currentDurationForEnd) < 0) {
                    updated = true;
                }
            } else {
                updated = true;
                searchState.addCostFor(endRelationshipNodeId, newCost, continuePath);
            }

            if (updated) {

                if (config.getDepthFirst()) {
                    // TODO ordering of which neighbours to visit first
                    visitNode(endRelationshipNodeId, graphStateForChildren, continuePath, reachedDest);
                }
                 else {
                    searchState.updateCostFor(endRelationshipNodeId, newCost, continuePath);
                }
            }

        });
    }

    private Stream<GraphRelationship> expand(final GraphPath path, final HasJourneyState graphState) {
        // TODO correct relationship types here

        final ImmutableJourneyState currentJourneyState = graphState.getJourneyState();
        final ImmutableTraversalState currentTraversalState = currentJourneyState.getTraversalState();

        if (currentTraversalState.getStateType() == TraversalStateType.NotStartedState) {

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
            return currentCost.getOrDefault(nodeId, notVisitiedDuration);
        }

        public void updateCostFor(GraphNodeId nodeId, Duration duration, GraphPathInMemory graphPath) {
            final NodeSearchState update = new NodeSearchState(nodeId, duration, graphPath);
            synchronized (nodeQueue) {
                nodeQueue.remove(update);
                nodeQueue.add(update);
                currentCost.put(nodeId, duration);
            }
        }

        public void addCostFor(GraphNodeId nodeId, Duration duration, GraphPathInMemory continuePath) {
            final NodeSearchState update = new NodeSearchState(nodeId, duration, continuePath);
            synchronized (nodeQueue) {
                nodeQueue.add(update);
                currentCost.put(nodeId, duration);
            }
        }

        public boolean hasNodes() {
            return !nodeQueue.isEmpty();
        }

        public boolean hasSeen(final GraphNodeId endNodeId) {
            return currentCost.containsKey(endNodeId);
        }

    }

    private static class NodeSearchState implements Comparable<NodeSearchState> {
        private final GraphNodeId nodeId;
        private final Duration duration;
        private final GraphPathInMemory pathToHere;

        private NodeSearchState(GraphNodeId nodeId, Duration duration, GraphPathInMemory pathToHere) {
            this.nodeId = nodeId;
            this.duration = duration;
            this.pathToHere = pathToHere;
        }

//        public NodeState(GraphNodeId id, GraphPathInMemory pathTodHere) {
//            this(id, maxDuration, pathTodHere);
//        }

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

        public Duration getDuration() {
            return duration;
        }

        public GraphPathInMemory getPathToHere() {
            return pathToHere;
        }
    }

}
