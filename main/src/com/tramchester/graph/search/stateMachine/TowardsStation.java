package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.GraphNode;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.states.GroupedStationState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StationState;
import com.tramchester.graph.search.stateMachine.states.WalkingState;

import java.time.Duration;

public interface TowardsStation<T extends StationState> extends Towards<T> {
    T fromNeighbour(StationState stationState, GraphNode next, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion);
    T fromStart(NotStartedState notStartedState, GraphNode firstNode, Duration cost, JourneyStateUpdate journeyState, boolean alreadyOnDiversion, boolean onDiversion);
    T fromWalking(WalkingState walkingState, GraphNode node, Duration cost, JourneyStateUpdate journeyState);
    T fromGrouped(GroupedStationState groupedStationState, GraphNode next, Duration cost, JourneyStateUpdate journeyState);
}
