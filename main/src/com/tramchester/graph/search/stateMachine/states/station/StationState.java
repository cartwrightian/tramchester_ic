package com.tramchester.graph.search.stateMachine.states.station;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.NodeId;
import com.tramchester.graph.search.stateMachine.journeyState.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;
import com.tramchester.graph.search.stateMachine.journeyState.TraversalStateType;
import com.tramchester.graph.search.stateMachine.states.*;

import java.util.stream.Stream;

public abstract class StationState extends TraversalState implements NodeId {

    public abstract static class StationBuilder<T extends StationState> extends StateBuilder<T> implements TowardsStation<T> {

        protected StationBuilder(StateBuilderParameters parameters) {
            super(parameters);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.WalkingState, this);
            registers.add(TraversalStateType.NotStartedState, this);

            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.PlatformStationState, this);

            registers.add(TraversalStateType.GroupedStationState, this);
        }

    }

    protected final GraphNode stationNode;
    private final IdFor<Station> stationId;

    protected StationState(final ImmutableTraversalState parent, final Stream<GraphRelationship> outbounds,
                           final TramDuration costForLastEdge, final GraphNode stationNode,
                           final JourneyStateUpdate journeyState, final TraversalStateType builderDestinationType) {
        super(parent, outbounds, costForLastEdge, builderDestinationType, stationNode.getId());
        this.stationNode = stationNode;
        this.stationId = stationNode.getStationId();
        journeyState.recordStation(stationNode.getStationId());
    }

    public IdFor<Station> getStationId() {
        return stationId;
    }

    @Override
    public GraphNodeId nodeId() {
        return stationNode.getId();
    }

    @Override
    protected void toDestination(final DestinationState.Builder towardsDestination, final GraphNode node,
                                 final TramDuration cost, final JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost, node, journeyStateUpdate);
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode next,
                                                     final TramDuration cost, final boolean viaDivert,
                                                     final JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost, journeyState, txn, viaDivert);
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder toStation, final GraphNode node,
                                                 final TramDuration cost, final boolean viaDivert,
                                                 final JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, node, cost);
        return toStation.fromNeighbour(this, node, cost, journeyState, txn, viaDivert);
    }

    @Override
    protected TraversalState toWalk(final WalkingState.Builder towardsWalk, final GraphNode node, final TramDuration cost,
                                    final JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, cost);
        return towardsWalk.fromStation(this, node, cost, txn);
    }

    @Override
    protected TraversalState toGrouped(final GroupedStationState.Builder towardsGroup, JourneyStateUpdate journeyStateUpdate,
                                       final GraphNode node, final TramDuration cost, final boolean viaDivert,
                                       final JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, journeyStateUpdate, node, cost, txn);
    }

}
