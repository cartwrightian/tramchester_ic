package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;

import java.time.Duration;
import java.util.stream.Stream;

public abstract class RouteStationState extends TraversalState {

    protected RouteStationState(ImmutableTraversalState parent, Stream<ImmutableGraphRelationship> outbounds,
                                JourneyStateUpdate journeyState,
                                Duration costForLastEdge, TowardsRouteStation<?> builder, GraphNode graphNode) {
        super(parent, outbounds, costForLastEdge, builder.getDestination(), graphNode);
        journeyState.seenRouteStation(graphNode.getStationId());


    }
}
