package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.journeyState.TraversalStateType;

public interface RegistersFromState {
    <T extends TraversalState> void add(TraversalStateType from, Towards<T> builder);
}
