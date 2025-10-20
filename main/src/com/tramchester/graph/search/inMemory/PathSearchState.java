package com.tramchester.graph.search.inMemory;

import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.search.JourneyState;

import java.time.Duration;
import java.util.*;

class PathSearchState {
    // remaining working
    private final PriorityQueue<NodeSearchState> nodeQueue;
    // state
    private final Map<GraphNodeId, Duration> currentCost;
    private final Map<GraphNodeId, JourneyState> journeyStates;
    // results
    final List<GraphPathInMemory> foundPaths;

    PathSearchState(final GraphNodeId startNodeId, final GraphPathInMemory pathToHere) {
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
        return currentCost.getOrDefault(nodeId, FindPathsForJourney.NotVisitiedDuration);
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
        // can this get out of sink with the nodeQueue? i.e. have a cost but not corresponding node in the queue?
        synchronized (nodeQueue) {
            return currentCost.containsKey(endNodeId);
        }
    }

    public void updateCostAndQueue(final GraphNodeId graphNodeId, final Duration duration, final GraphPathInMemory graphPath) {
        final NodeSearchState update = new NodeSearchState(graphNodeId, duration, graphPath);

        synchronized (nodeQueue) {
            // clunky, relies on NodeSearchState defining equals to be on NodeId only
            if (nodeQueue.contains(update)) {
                nodeQueue.remove(update);
                nodeQueue.add(update);
            } else {
                throw new RuntimeException("Node was not in the queue " + graphNodeId + " for " + update);
            }
            currentCost.put(graphNodeId, duration);
        }
    }

    public void addCostAndQueue(GraphNodeId graphNodeId, Duration duration, GraphPathInMemory graphPath) {
        final NodeSearchState update = new NodeSearchState(graphNodeId, duration, graphPath);

        addCostAndQueue(update);
    }

    private void addCostAndQueue(final NodeSearchState update) {
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

    public static class NodeSearchState implements Comparable<NodeSearchState> {
        private final GraphNodeId nodeId;
        private final Duration duration;
        private final GraphPathInMemory pathToHere;

        NodeSearchState(GraphNodeId nodeId, Duration duration, GraphPathInMemory pathToHere) {
            this.nodeId = nodeId;
            this.duration = duration;
            this.pathToHere = pathToHere.duplicateThis();
        }

        @Override
        public int compareTo(final NodeSearchState other) {
            // depth first
            return Integer.compare(other.pathToHere.length(), pathToHere.length());

            //return this.duration.compareTo(other.duration);
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

        public Duration getDuration() {
            return duration;
        }
    }

}
