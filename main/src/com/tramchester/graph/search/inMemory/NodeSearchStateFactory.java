package com.tramchester.graph.search.inMemory;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;
import org.jetbrains.annotations.NotNull;

public class NodeSearchStateFactory {
    public NodeSearchState createNodeSearchState(@NotNull SearchStateKey stateKey, TramDuration tramDuration,
                                                 GraphPathInMemory path, boolean towardsDest) {
        return NodeSearchState.createNodeSearchState(stateKey, tramDuration, path, towardsDest);
    }

    public NodeSearchState createInitialState(SearchStateKey stateKey, GraphPathInMemory pathToHere) {
        return NodeSearchState.createInitialState(stateKey, pathToHere);
    }
}
