package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;
import java.util.EnumSet;
import java.util.stream.Stream;

public interface ImmutableTraversalState {

    ImmutableTraversalState nextState(EnumSet<GraphLabel> nodeLabels, GraphNode node,
                             JourneyStateUpdate journeyState, Duration duration);

    Duration getTotalDuration();

    TraversalStateType getStateType();

    GraphNodeId nodeId();

    GraphTransaction getTransaction();

    TraversalStateFactory getTraversalStateFactory();

    Stream<ImmutableGraphRelationship> getOutbounds();
}
