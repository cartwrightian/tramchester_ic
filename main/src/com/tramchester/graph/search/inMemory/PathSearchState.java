package com.tramchester.graph.search.inMemory;

import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.search.JourneyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

class PathSearchState {
    private static final Logger logger = LoggerFactory.getLogger(PathSearchState.class);

    // remaining working
    private final PriorityQueue<NodeSearchState> nodeQueue;
    // state
    private final Map<SearchStateKey, Duration> currentCost;
    private final Map<SearchStateKey, JourneyState> journeyStates;
    // results
    final List<GraphPathInMemory> foundPaths;
    private final long numberJourneys;

    PathSearchState(final SearchStateKey searchStateKey, final GraphPathInMemory pathToHere, long numberJourneys) {
        this.numberJourneys = numberJourneys;
        nodeQueue = new PriorityQueue<>();
        nodeQueue.add(new NodeSearchState(searchStateKey, Duration.ZERO, pathToHere));

        currentCost = new HashMap<>();
        currentCost.put(searchStateKey, Duration.ZERO);

        foundPaths = new ArrayList<>();
        journeyStates = new HashMap<>();
    }

    public void clear() {
        nodeQueue.clear();
        currentCost.clear();
        journeyStates.clear();
        foundPaths.clear();
    }

    public NodeSearchState getNext() {
        return nodeQueue.poll();
    }

    public Duration getCurrentCost(final SearchStateKey stateKey) {
        return currentCost.getOrDefault(stateKey, FindPathsForJourney.NotVisitiedDuration);
    }

    public void updateCost(final SearchStateKey stateKey, final Duration duration) {
        synchronized (nodeQueue) {
            currentCost.put(stateKey, duration);
        }
    }

    public boolean hasSeen(final SearchStateKey stateKey) {
        // can this get out of sink with the nodeQueue? i.e. have a cost but not corresponding node in the queue?
        synchronized (nodeQueue) {
            return currentCost.containsKey(stateKey);
        }
    }

    public void updateCostAndQueue(final SearchStateKey stateKey, final Duration duration, final GraphPathInMemory graphPath) {
        final NodeSearchState update = new NodeSearchState(stateKey, duration, graphPath);

        synchronized (nodeQueue) {
            // clunky, relies on NodeSearchState defining equals to be on NodeId only
            if (nodeQueue.contains(update)) {
                nodeQueue.remove(update);
                nodeQueue.add(update);
            } else {
                String message = "Node was not in the queue " + stateKey + " for " + update;
                logger.error(message);
                throw new RuntimeException(message);
            }
            currentCost.put(stateKey, duration);
        }
    }

    public void addCostAndQueue(SearchStateKey stateKey, Duration duration, GraphPathInMemory graphPath) {
        final NodeSearchState update = new NodeSearchState(stateKey, duration, graphPath);

        synchronized (nodeQueue) {
            if (nodeQueue.contains(update)) {
                String message = "Already in queue " + update.getStateKey();
                logger.error(message);
                throw new RuntimeException(message);
            }
            nodeQueue.add(update);
            currentCost.put(stateKey, duration);
        }
    }

    public List<GraphPathInMemory> getFoundPaths() {
        return new LinkedList<>(foundPaths);
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

    public void setJourneyState(final SearchStateKey id, final JourneyState journeyState) {
        journeyStates.put(id, journeyState);
    }

    public JourneyState getJourneyStateFor(final SearchStateKey stateKey) {
        if (!journeyStates.containsKey(stateKey)) {
            String message = "No journey state for " + stateKey;
            logger.error(message);
            throw new RuntimeException(message);
        }
        return journeyStates.get(stateKey);
    }

    public boolean continueSearch() {
        if (nodeQueue.isEmpty()) {
            logger.warn("Queue was empty");
            return false;
        }
        synchronized (foundPaths) {
            if (foundPaths.size()>=numberJourneys) {
                logger.info("Matched " + numberJourneys + " journeys");
                return false;
            }
        }
        return true;
    }

    public static class NodeSearchState implements Comparable<NodeSearchState> {
        private final SearchStateKey stateKey;
        private final Duration duration;
        private final GraphPathInMemory pathToHere;

        NodeSearchState(SearchStateKey stateKey, Duration duration, GraphPathInMemory pathToHere) {
            this.stateKey = stateKey;
            this.duration = duration;
            this.pathToHere = pathToHere.duplicateThis();
        }

        @Override
        public int compareTo(final NodeSearchState other) {
            // depth first
            return Integer.compare(other.pathToHere.length(), pathToHere.length());
        }

        public SearchStateKey getStateKey() {
            return stateKey;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            NodeSearchState nodeSearchState = (NodeSearchState) o;
            return Objects.equals(stateKey, nodeSearchState.stateKey);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(stateKey);
        }

        public GraphPathInMemory getPathToHere() {
            return pathToHere;
        }

        @Override
        public String toString() {
            return "NodeSearchState{" +
                    "stateKey=" + stateKey +
                    ", duration=" + duration +
                    ", pathToHere=" + pathToHere +
                    '}';
        }

        public Duration getDuration() {
            return duration;
        }
    }

}
