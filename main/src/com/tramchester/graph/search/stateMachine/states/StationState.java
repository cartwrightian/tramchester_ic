package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.TowardsStation;

import java.time.Duration;
import java.util.stream.Stream;

public abstract class StationState extends TraversalState implements NodeId {

    protected final GraphNode stationNode;

    protected StationState(TraversalState parent, Stream<ImmutableGraphRelationship> outbounds, Duration costForLastEdge, GraphNode stationNode,
                           JourneyStateUpdate journeyState, TowardsStation<?> builder) {
        super(parent, outbounds, costForLastEdge, builder.getDestination());
        this.stationNode = stationNode;
        journeyState.seenStation(stationNode.getStationId());
    }

}
