package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;
import java.util.EnumSet;

public interface ImmutableTraversalState {

    Duration getTotalDuration();

    TraversalState nextState(EnumSet<GraphLabel> nodeLabels, GraphNode node,
                             JourneyStateUpdate journeyState, Duration duration, boolean alreadyOnDiversion);

    TraversalStateType getStateType();

    GraphNodeId nodeId();
}
