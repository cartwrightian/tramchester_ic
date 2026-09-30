package com.tramchester.graph.search.stateMachine.states.routeStation;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.search.stateMachine.journeyState.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;

import java.util.stream.Stream;

public abstract class RouteStationState extends TraversalState {

    public enum PassType {
        JustBoarded, OnTrip, EndTrip
    }

    protected RouteStationState(ImmutableTraversalState parent, Stream<GraphRelationship> outbounds,
                                JourneyStateUpdate journeyState,
                                TramDuration costForLastEdge, TowardsRouteStation<?> builder,
                                GraphNode graphNode, PassType passType) {
        super(parent, outbounds, costForLastEdge, builder.getDestination(), graphNode.getId());
        journeyState.recordRouteStation(graphNode, passType);
    }
}
