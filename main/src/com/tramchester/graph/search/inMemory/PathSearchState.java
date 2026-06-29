package com.tramchester.graph.search.inMemory;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;
import com.tramchester.graph.search.JourneyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PathSearchState {
    private static final Logger logger = LoggerFactory.getLogger(PathSearchState.class);

    // remaining working
    private final PriorityQueue<NodeSearchState> nodeQueue;
    // state
    private final Map<SearchStateKey, TramDuration> currentCost;
    private final Map<SearchStateKey, JourneyState> journeyStates;
    // results
    final List<GraphPathInMemory> foundPaths;
    private final long numberJourneys;

    PathSearchState(final SearchStateKey searchStateKey, final GraphPathInMemory pathToHere, long numberJourneys) {
        this.numberJourneys = numberJourneys;
        nodeQueue = new PriorityQueue<>();
        nodeQueue.add(NodeSearchState.createInitialState(searchStateKey, pathToHere));

        currentCost = new HashMap<>();
        currentCost.put(searchStateKey, TramDuration.ZERO);

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

    public TramDuration getCurrentCost(final SearchStateKey stateKey) {
        return currentCost.getOrDefault(stateKey, FindPathsForJourney.NotVisitedDuration);
    }

    public void updateCost(final SearchStateKey stateKey, final TramDuration duration) {
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



    public void updateCostAndQueue(final SearchStateKey stateKey, final TramDuration duration, final GraphPathInMemory graphPath, boolean towardsDest) {
        final NodeSearchState update = NodeSearchState.createNodeSearchState(stateKey, duration, graphPath, towardsDest);

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

    public void addCostAndQueue(final SearchStateKey stateKey, final TramDuration duration, final GraphPathInMemory graphPath,
                                final boolean towardsDest) {
        final NodeSearchState update = NodeSearchState.createNodeSearchState(stateKey, duration, graphPath, towardsDest);

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

}
