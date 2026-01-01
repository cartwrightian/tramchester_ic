package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.util.EnumSet;
import java.util.stream.Stream;

public interface ImmutableTraversalState {

    ImmutableTraversalState nextState(EnumSet<GraphLabel> nodeLabels, GraphNode node,
                             JourneyStateUpdate journeyState, TramDuration duration);

    TramDuration getTotalDuration();

    TraversalStateType getStateType();

    GraphNodeId nodeId();

    GraphTransaction getTransaction();

    TraversalStateFactory getTraversalStateFactory();

    Stream<GraphRelationship> getOutbounds();
}
