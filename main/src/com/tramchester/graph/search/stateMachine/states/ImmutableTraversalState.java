package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TraversalOps;

import java.time.Duration;
import java.util.EnumSet;
import java.util.stream.Stream;

public interface ImmutableTraversalState {

    ImmutableTraversalState nextState(EnumSet<GraphLabel> nodeLabels, GraphNode node,
                             JourneyStateUpdate journeyState, Duration duration);

    Duration getTotalDuration();

    TraversalStateType getStateType();

    GraphNodeId nodeId();

    TraversalOps getTraversalOps();

    GraphTransaction getTransaction();

    TraversalStateFactory getTraversalStateFactory();

    Stream<ImmutableGraphRelationship> getOutbounds(GraphTransaction txn);
}
