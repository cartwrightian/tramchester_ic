package com.tramchester.graph.search.inMemory;


import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.GraphTransactionInMemory;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

public class SpikeAlgo {

    private static final Logger logger = LoggerFactory.getLogger(SpikeAlgo.class);

    private final GraphTransactionInMemory txn;
    private final GraphNode startNode;
    private final GraphNode destinationNode;
    private final TramchesterConfig config;

    private static final Duration maxDuration = Duration.ofSeconds(Integer.MAX_VALUE);

    public SpikeAlgo(GraphTransaction txn, GraphNode startNode, GraphNode destinationNode, TramchesterConfig config) {
        this.txn = (GraphTransactionInMemory) txn;
        this.startNode = startNode;
        this.destinationNode = destinationNode;
        this.config = config;
    }

    public List<GraphPath> findRoute() {
        final State state = new State(startNode.getId(), new GraphPathInMemory());

        final List<GraphPathInMemory> paths = new ArrayList<>();

        while (state.hasNodes()) {
            final NodeState nodeState = state.getNext();
            final GraphNodeId nextId = nodeState.getNodeId();
            visitNode(nextId, state, nodeState.pathToHere, paths);
        }

        return paths.stream().map(item -> (GraphPath) item).toList();
    }

    // TODO the path to here
    private void visitNode(final GraphNodeId nodeId, final State state, GraphPathInMemory pathSoFar, List<GraphPathInMemory> reachedDest) {

        final GraphNode currentNode = txn.getNodeById(nodeId);

        final GraphPathInMemory pathToCurrentNode = pathSoFar.duplicateWith(txn, currentNode);

        if (currentNode.equals(destinationNode)) {
            // done, return current path up the stack
            logger.info("Found destination");
            reachedDest.add(pathToCurrentNode);
        }

        final Duration currentCostToNode = state.getCurrentCost(nodeId); //pair.getDuration();

        final Stream<GraphRelationship> outgoing = currentNode.getRelationships(txn, GraphDirection.Outgoing,
                TransportRelationshipTypes.forPlanning());

        outgoing.forEach(graphRelationship -> {
            final Duration relationshipCost = graphRelationship.getCost();
            final GraphNode endRelationshipNode = graphRelationship.getEndNode(txn);
            final GraphNodeId endRelationshipNodeId = endRelationshipNode.getId();
            final Duration newCost = relationshipCost.plus(currentCostToNode);

            boolean updated = false;
            final GraphPathInMemory continuePath = pathToCurrentNode.duplicateWith(txn, graphRelationship);
            if (state.hasSeen(endRelationshipNodeId)) {
                final Duration currentDurationForEnd = state.getCurrentCost(endRelationshipNodeId);
                if (newCost.compareTo(currentDurationForEnd) < 0) {
                    updated = true;
                }
            } else {
                updated = true;
                state.addCostFor(endRelationshipNodeId, newCost, continuePath);
            }

            if (updated) {

                if (config.getDepthFirst()) {
                    // TODO ordering of which neighbours to visit first
                    visitNode(endRelationshipNodeId, state, continuePath, reachedDest);
                }
                 else {
                    state.updateCostFor(endRelationshipNodeId, newCost, continuePath);
                }
            }

        });
    }

    private static class State {
        private final PriorityQueue<NodeState> nodeQueue;
        private final Map<GraphNodeId, Duration> currentCost;

        private State(GraphNodeId startNodeId, GraphPathInMemory pathToHere) {
            nodeQueue = new PriorityQueue<>();
            nodeQueue.add(new NodeState(startNodeId, Duration.ZERO, pathToHere));
            currentCost = new HashMap<>();
        }

        public NodeState getNext() {
            return nodeQueue.poll();
        }

        public Duration getCurrentCost(final GraphNodeId nodeId) {
            return currentCost.getOrDefault(nodeId, maxDuration);
        }

        public void updateCostFor(GraphNodeId nodeId, Duration duration, GraphPathInMemory graphPath) {
            final NodeState update = new NodeState(nodeId, duration, graphPath);
            synchronized (nodeQueue) {
                nodeQueue.remove(update);
                nodeQueue.add(update);
                currentCost.put(nodeId, duration);
            }
        }

        public void addCostFor(GraphNodeId nodeId, Duration duration, GraphPathInMemory continuePath) {
            final NodeState update = new NodeState(nodeId, duration, continuePath);
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

    private static class NodeState implements Comparable<NodeState> {
        private final GraphNodeId nodeId;
        private final Duration duration;
        private final GraphPathInMemory pathToHere;

        private NodeState(GraphNodeId nodeId, Duration duration, GraphPathInMemory pathToHere) {
            this.nodeId = nodeId;
            this.duration = duration;
            this.pathToHere = pathToHere;
        }

        public NodeState(GraphNodeId id, GraphPathInMemory pathTodHere) {
            this(id, maxDuration, pathTodHere);
        }

        @Override
        public int compareTo(NodeState other) {
            return this.duration.compareTo(other.duration);
        }

        public GraphNodeId getNodeId() {
            return nodeId;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            NodeState nodeState = (NodeState) o;
            return Objects.equals(nodeId, nodeState.nodeId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(nodeId);
        }

        public Duration getDuration() {
            return duration;
        }
    }

}
