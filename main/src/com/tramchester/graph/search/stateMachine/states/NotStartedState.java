package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TraversalOps;

import java.time.Duration;
import java.util.EnumSet;

public class NotStartedState extends TraversalState {

    public NotStartedState(final TraversalOps traversalOps, final TraversalStateFactory traversalStateFactory,
                           final EnumSet<TransportMode> requestedModes, final GraphNode graphNode) {
        super(traversalOps, traversalStateFactory, requestedModes, TraversalStateType.NotStartedState, graphNode);
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
    protected TraversalState toGrouped(final GroupedStationState.Builder towardsGroup, final GraphNode node, final Duration cost, final JourneyStateUpdate journeyState) {
        return towardsGroup.fromStart(this, node, cost, txn);
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                     final JourneyStateUpdate journeyState, final boolean onDiversion) {
        return towardsStation.fromStart(this, node, cost, journeyState, onDiversion, onDiversion, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyState, final boolean onDiversion) {
        return towardsStation.fromStart(this, node, cost, journeyState, onDiversion, onDiversion, txn);
    }
}
