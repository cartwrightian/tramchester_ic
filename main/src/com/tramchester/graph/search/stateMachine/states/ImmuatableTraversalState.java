package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.GraphNode;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;
import java.util.EnumSet;

public interface ImmuatableTraversalState {

    Duration getTotalDuration();

    TraversalState nextState(EnumSet<GraphLabel> nodeLabels, GraphNode node,
                             JourneyStateUpdate journeyState, Duration duration, boolean alreadyOnDiversion);

    TraversalStateType getStateType();
}
