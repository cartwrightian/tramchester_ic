package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;

public class NotStartedState extends TraversalState {

    public NotStartedState(final TraversalStateFactory traversalStateFactory,
                           final GraphNodeId graphNodeId, GraphTransaction txn) {
        super(traversalStateFactory, TraversalStateType.NotStartedState, graphNodeId, txn);
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
    protected TraversalState toWalk(final WalkingState.Builder towardsWalk, final GraphNode node, final Duration cost, final JourneyStateUpdate journeyState) {
        journeyState.beginWalk(node, true, cost);
        return towardsWalk.fromStart(this, node, cost, txn);
    }

    @Override
    protected TraversalState toGrouped(final GroupedStationState.Builder towardsGroup, JourneyStateUpdate journeyStateUpdate, final GraphNode node, final Duration cost, final JourneyStateUpdate journeyState) {
        return towardsGroup.fromStart(this, node, journeyStateUpdate, cost, txn);
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                     final JourneyStateUpdate journeyState) {
        return towardsStation.fromStart(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyState) {
        return towardsStation.fromStart(this, node, cost, journeyState, txn);
    }
}
