package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.GraphLabels;
import com.tramchester.graph.search.stateMachine.journeyState.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.journeyState.TraversalStateType;

import java.util.stream.Stream;

public interface ImmutableTraversalState {

    ImmutableTraversalState nextState(GraphLabels nodeLabels, GraphNode node,
                                      JourneyStateUpdate journeyState, TramDuration duration,
                                      boolean arrivedViaDiversion);

    TramDuration getTotalDuration();

    TraversalStateType getStateType();

    GraphNodeId nodeId();

    GraphTransaction getTransaction();

    TraversalStateFactory getTraversalStateFactory();

    Stream<GraphRelationship> getOutbounds();
}
