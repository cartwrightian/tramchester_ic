package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TraversalOps;

import java.time.Duration;
import java.util.Set;

public class NotStartedState extends TraversalState {

    public NotStartedState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory, Set<TransportMode> requestedModes) {
        super(traversalOps, traversalStateFactory, requestedModes, TraversalStateType.NotStartedState);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    @Override
    public Duration getTotalDuration() {
        return Duration.ZERO;
    }

    @Override
    public TraversalStateType getStateType() {
        return TraversalStateType.NotStartedState;
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(node, true, cost);
        return towardsWalk.fromStart(this, node, cost, txn);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromStart(this, node, cost, txn);
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromStart(this, node, cost, journeyState, onDiversion, onDiversion, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromStart(this, node, cost, journeyState, onDiversion, onDiversion, txn);
    }
}
