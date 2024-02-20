package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;

public abstract class EmptyTraversalState {

    protected final TraversalStateType stateType;

    protected EmptyTraversalState(TraversalStateType stateType) {
        this.stateType = stateType;
    }

    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toWalk(WalkingState.Builder towardsWalk, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, GraphNode node, Duration cost, boolean alreadyOnDiversion, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toService(ServiceState.Builder towardsService, GraphNode serviceNode, Duration cost) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, GraphNode node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected TraversalState toMinute(MinuteState.Builder towardsMinute, GraphNode node, Duration cost, JourneyStateUpdate journeyState, TransportRelationshipTypes[] currentModes) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected HourState toHour(HourState.Builder towardsHour, GraphNode node, Duration cost) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected RouteStationStateOnTrip toRouteStationOnTrip(RouteStationStateOnTrip.Builder towardsRouteStation,
                                                           JourneyStateUpdate journeyState, GraphNode node, Duration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + stateType);
    }

    protected RouteStationStateEndTrip toRouteStationEndTrip(RouteStationStateEndTrip.Builder towardsRouteStation,
                                                             JourneyStateUpdate journeyState, GraphNode node, Duration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + stateType);
    }

}
