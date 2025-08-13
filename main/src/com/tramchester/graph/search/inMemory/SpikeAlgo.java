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

    public GraphPath findRoute() {
        State state = new State(startNode.getId());

        while (state.hasNodes()) {
            final NodePair pair = state.getNext();
            final GraphNodeId nextId = pair.getNodeId();
            visitNode(nextId, state);
        }

        return new GraphPathInMemory();
    }

    // TODO the path to here
    private void visitNode(final GraphNodeId nodeId, final State state) {

        final GraphNode next = txn.getNodeById(nodeId);
        if (next.equals(destinationNode)) {
            // done
            logger.info("Found destination");
            return;
        }

        final Duration currentCostToNode = state.getCurrentCost(nodeId); //pair.getDuration();

        final Stream<GraphRelationship> outgoing = next.getRelationships(txn, GraphDirection.Outgoing,
                TransportRelationshipTypes.forPlanning());

        outgoing.forEach(graphRelationship -> {
            final Duration relationshipCost = graphRelationship.getCost();
            final GraphNode endNode = graphRelationship.getEndNode(txn);
            final GraphNodeId endNodeId = endNode.getId();
            final Duration newCost = relationshipCost.plus(currentCostToNode);

            boolean updated = false;
            if (state.hasSeen(endNodeId)) {
                final Duration currentDurationForEnd = state.getCurrentCost(endNodeId);
                if (newCost.compareTo(currentDurationForEnd) < 0) {
                    updated = true;
                    state.updateCostFor(endNodeId, newCost);
                }
            } else {
                updated = true;
                state.addCostFor(endNodeId, newCost);
            }

            if (updated) {
                if (config.getDepthFirst()) {
                    // TODO ordering of which neighbours to visit first
                    visitNode(endNodeId, state);
                } // else fallback to global ordering??
            }

        });
    }

    private static class State {
        private final PriorityQueue<NodePair> nodeQueue;
        private final Map<GraphNodeId, Duration> currentCost;

        private State(GraphNodeId startNodeId) {
            nodeQueue = new PriorityQueue<>();
            nodeQueue.add(new NodePair(startNodeId, Duration.ZERO));
            currentCost = new HashMap<>();
        }

        public NodePair getNext() {
            return nodeQueue.poll();
        }

        public Duration getCurrentCost(final GraphNodeId nodeId) {
            return currentCost.getOrDefault(nodeId, maxDuration);
        }

        public void updateCostFor(GraphNodeId nodeId, Duration duration) {
            final NodePair update = new NodePair(nodeId, duration);
            synchronized (nodeQueue) {
                nodeQueue.remove(update);
                nodeQueue.add(update);
                currentCost.put(nodeId, duration);
            }
        }

        public void addCostFor(GraphNodeId nodeId, Duration duration) {
            final NodePair update = new NodePair(nodeId, duration);
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

    private static class NodePair implements Comparable<NodePair> {
        private final GraphNodeId nodeId;
        private final Duration duration;

        private NodePair(GraphNodeId nodeId, Duration duration) {
            this.nodeId = nodeId;
            this.duration = duration;
        }

        public NodePair(GraphNodeId id) {
            this(id, maxDuration);
        }

        @Override
        public int compareTo(NodePair other) {
            return this.duration.compareTo(other.duration);
        }

        public GraphNodeId getNodeId() {
            return nodeId;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            NodePair nodePair = (NodePair) o;
            return Objects.equals(nodeId, nodePair.nodeId);
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
