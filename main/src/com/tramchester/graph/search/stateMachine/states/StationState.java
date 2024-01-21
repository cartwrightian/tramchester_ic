package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;

import java.time.Duration;
import java.util.stream.Stream;

public abstract class StationState extends TraversalState implements NodeId {

    protected final GraphNode stationNode;

    protected StationState(final TraversalState parent, final Stream<ImmutableGraphRelationship> outbounds, final Duration costForLastEdge,
                           final GraphNode stationNode,
                           final JourneyStateUpdate journeyState, final TraversalStateType builderDestinationType) { //TowardsStation<?> builder) {
        super(parent, outbounds, costForLastEdge, builderDestinationType); // builder.getDestination());
        this.stationNode = stationNode;
        journeyState.seenStation(stationNode.getStationId());
    }

}
