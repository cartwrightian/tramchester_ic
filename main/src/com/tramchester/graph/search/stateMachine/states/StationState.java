package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.TowardsStation;

import java.time.Duration;
import java.util.stream.Stream;

public abstract class StationState extends TraversalState implements NodeId {

    protected final GraphNode stationNode;

    protected StationState(TraversalState parent, Stream<GraphRelationship> outbounds, Duration costForLastEdge, GraphNode stationNode,
                           JourneyStateUpdate journeyState, TowardsStation<?> builder) {
        super(parent, outbounds, costForLastEdge, builder.getDestination());
        this.stationNode = stationNode;
        journeyState.seenStation(GraphProps.getStationId(stationNode));
    }

//    protected StationState(TraversalState parent, ResourceIterable<Relationship> outbounds, Duration costForLastEdge, GraphNode stationNode,
//                           JourneyStateUpdate journeyState, TowardsStation<?> builder) {
//        super(parent, outbounds, costForLastEdge, builder.getDestination());
//        this.stationNode = stationNode;
//        journeyState.seenStation(GraphProps.getStationId(stationNode));
//    }
}
