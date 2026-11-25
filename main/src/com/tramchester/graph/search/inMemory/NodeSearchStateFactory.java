package com.tramchester.graph.search.inMemory;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;

import java.util.function.BiFunction;

public class NodeSearchStateFactory<T> {

    private final BiFunction<T, T, Integer> comparison;

    public NodeSearchStateFactory(BiFunction<T, T, Integer> comparison) {
        this.comparison = comparison;
    }

    public NodeSearchState<T> createNodeSearchState(SearchStateKey stateKey, TramDuration tramDuration,
                                                 GraphPathInMemory path, boolean towardsDest, T metric) {
        return NodeSearchState.createNodeSearchState(stateKey, tramDuration, path, towardsDest, metric, comparison);
    }

    public NodeSearchState<T> createInitialState(SearchStateKey stateKey, GraphPathInMemory pathToHere, T metric) {
        return NodeSearchState.createInitialState(stateKey, pathToHere, metric, comparison);
    }
}
