package com.tramchester.graph.search.inMemory;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.BiFunction;

public class NodeSearchState<T> implements Comparable<NodeSearchState<T>> {
    private final SearchStateKey stateKey;
    private final TramDuration duration;
    private final GraphPathInMemory pathToHere;
    private final boolean towardsDest;
    private final T metric;
    private final BiFunction<T, T, Integer> comparison;

    private NodeSearchState(SearchStateKey stateKey, TramDuration duration, GraphPathInMemory pathToHere, boolean towardsDest, T metric,
                            BiFunction<T, T, Integer> comparison) {
        this.stateKey = stateKey;
        this.duration = duration;
        this.pathToHere = pathToHere.duplicate();
        this.towardsDest = towardsDest; // used when we can id states that lead directly to a destination
        this.metric = metric;
        this.comparison = comparison;
    }

    static <COMPARE> NodeSearchState<COMPARE> createNodeSearchState(final SearchStateKey endStateKey,
                                                final TramDuration newCost, final GraphPathInMemory continuePath,
                                                final boolean towardsDest, COMPARE metric, BiFunction<COMPARE, COMPARE, Integer> comparison) {
        return new NodeSearchState<>(endStateKey, newCost, continuePath, towardsDest, metric , comparison);
    }

    static <COMPARE> NodeSearchState<COMPARE> createInitialState(final SearchStateKey searchStateKey,
                                             final GraphPathInMemory pathToHere, COMPARE metric, BiFunction<COMPARE, COMPARE, Integer> comparison) {
        return new NodeSearchState<>(searchStateKey, TramDuration.ZERO, pathToHere, false, metric , comparison);
    }

    @Override
    public int compareTo(final @NonNull NodeSearchState other) {
        if (towardsDest && other.towardsDest) {
            // shortest path here
            return compareWith(this, other);
        }
        if (towardsDest) {
            return -1;
        }
        if (other.towardsDest) {
            return 1;
        }
        // depth first - longest path comes first
        return compareWith(other, this);
    }

    private int compareWith(final NodeSearchState<T> stateA, final NodeSearchState<T> stateB) {
        return comparison.apply(stateA.metric, stateB.metric);
    }

    public SearchStateKey getStateKey() {
        return stateKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NodeSearchState<T> nodeSearchState = (NodeSearchState) o;
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

//    public boolean getTowardsDest() {
//        return towardsDest;
//    }
}
