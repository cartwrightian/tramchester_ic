package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.search.JourneyStateUpdate;

public abstract class EmptyTraversalState {

    protected final TraversalStateType stateType;

    protected EmptyTraversalState(TraversalStateType stateType) {
        this.stateType = stateType;
    }

    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, GraphNode node, TramDuration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toWalk(WalkingState.Builder towardsWalk, GraphNode node, TramDuration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, GraphNode node, TramDuration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toService(ServiceState.Builder towardsService, GraphNode serviceNode, TramDuration cost) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, GraphNode node, TramDuration cost,
                                                 JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, JourneyStateUpdate journeyStateUpdate, GraphNode node, TramDuration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toMinute(MinuteState.Builder towardsMinute, GraphNode node, TramDuration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, TramDuration cost,
                                                     JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode node, TramDuration cost, JourneyStateUpdate journeyStateUpdate) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected HourState toHour(HourState.Builder towardsHour, GraphNode node, TramDuration cost) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected RouteStationStateOnTrip toRouteStationOnTrip(RouteStationStateOnTrip.Builder towardsRouteStation,
                                                           JourneyStateUpdate journeyState, GraphNode node, TramDuration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected RouteStationStateEndTrip toRouteStationEndTrip(RouteStationStateEndTrip.Builder towardsRouteStation,
                                                             JourneyStateUpdate journeyState, GraphNode node, TramDuration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + stateType);
    }

}
