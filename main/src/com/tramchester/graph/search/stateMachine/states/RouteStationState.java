package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;

import java.util.stream.Stream;

public abstract class RouteStationState extends TraversalState {

    protected RouteStationState(ImmutableTraversalState parent, Stream<GraphRelationship> outbounds,
                                JourneyStateUpdate journeyState,
                                TramDuration costForLastEdge, TowardsRouteStation<?> builder, GraphNode graphNode) {
        super(parent, outbounds, costForLastEdge, builder.getDestination(), graphNode.getId());
        journeyState.seenRouteStation(graphNode);
    }
}
