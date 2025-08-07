package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;

import java.time.Duration;
import java.util.stream.Stream;

public abstract class StationState extends TraversalState implements NodeId {

    protected final GraphNode stationNode;
    private final IdFor<Station> stationId;

    protected StationState(final ImmutableTraversalState parent, final Stream<GraphRelationship> outbounds, final Duration costForLastEdge,
                           final GraphNode stationNode,
                           final JourneyStateUpdate journeyState, final TraversalStateType builderDestinationType) {
        super(parent, outbounds, costForLastEdge, builderDestinationType, stationNode.getId());
        this.stationNode = stationNode;
        this.stationId = stationNode.getStationId();
        journeyState.seenStation(stationNode.getStationId());
    }

    public IdFor<Station> getStationId() {
        return stationId;
    }

}
