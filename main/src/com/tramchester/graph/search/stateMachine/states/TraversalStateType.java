package com.tramchester.graph.search.stateMachine.states;

import java.util.EnumSet;

public enum TraversalStateType {
    DestinationState,
    GroupedStationState,
    HourState,
    MinuteState,
    NoPlatformStationState,
    NotStartedState,
    PlatformState,
    PlatformStationState,
    RouteStationStateEndTrip,
    RouteStationStateOnTrip,
    ServiceState,
    WalkingState,
    JustBoardedState;

    private final static EnumSet<TraversalStateType> stations = EnumSet.of(PlatformStationState, NoPlatformStationState);

    public static boolean isStation(final TraversalStateType type) {
        return stations.contains(type);
    }
}
