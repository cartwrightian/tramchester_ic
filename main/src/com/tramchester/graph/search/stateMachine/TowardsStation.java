package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.states.GroupedStationState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StationState;
import com.tramchester.graph.search.stateMachine.states.WalkingState;

public interface TowardsStation<T extends StationState> extends Towards<T> {
    T fromNeighbour(StationState stationState, GraphNode next, TramDuration cost, JourneyStateUpdate journeyState, GraphTransaction txn);
    T fromStart(NotStartedState notStartedState, GraphNode firstNode, TramDuration cost, JourneyStateUpdate journeyState, GraphTransaction txn);
    T fromWalking(WalkingState walkingState, GraphNode node, TramDuration cost, JourneyStateUpdate journeyState, GraphTransaction txn);
    T fromGrouped(GroupedStationState groupedStationState, GraphNode next, TramDuration cost, JourneyStateUpdate journeyState, GraphTransaction txn);
}
