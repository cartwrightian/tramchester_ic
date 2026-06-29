package com.tramchester.graph.search.inMemory;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;

import java.util.Objects;
import java.util.function.BinaryOperator;

public class NodeSearchState implements Comparable<NodeSearchState> {
    private final SearchStateKey stateKey;
    private final TramDuration duration;
    private final GraphPathInMemory pathToHere;
    private final boolean towardsDest;

    private NodeSearchState(SearchStateKey stateKey, TramDuration duration, GraphPathInMemory pathToHere, boolean towardsDest) {
        this.stateKey = stateKey;
        this.duration = duration;
        this.pathToHere = pathToHere.duplicate();
        this.towardsDest = towardsDest; // used when we can id states that lead directly to a destination
    }

    public static NodeSearchState createNodeSearchState(final SearchStateKey endStateKey, final TramDuration newCost,
                                                        final GraphPathInMemory continuePath, final boolean towardsDest) {
        return new NodeSearchState(endStateKey, newCost, continuePath, towardsDest);
    }

    public static NodeSearchState createInitialState(final SearchStateKey searchStateKey, final GraphPathInMemory pathToHere) {
        return new NodeSearchState(searchStateKey, TramDuration.ZERO, pathToHere, false);
    }

    @Override
    public int compareTo(final NodeSearchState other) {
        if (towardsDest && other.towardsDest) {
            // shortest path here
            return compareWith(other, Integer::compare);
        }
        if (towardsDest) {
            return -1;
        }
        if (other.towardsDest) {
            return 1;
        }
        // depth first - longest path comes first
        return compareWith(other, (a, b) -> Integer.compare(b, a));
    }

    private int compareWith(final NodeSearchState other, final BinaryOperator<Integer> comparison) {
        int result = comparison.apply(pathToHere.length(), other.pathToHere.length());
        if (result == 0) {
            // tie-break via duration (shortest wins)
            // TODO WIP - shortest first breaks some tram/train tests, digging into why
            //result = other.duration.compareTo(duration);
            result = duration.compareTo(other.duration);

        }
        return result;
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

    public TramDuration getDuration() {
        return duration;
    }

    public boolean getTowardsDest() {
        return towardsDest;
    }
}
