package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.states.*;

import java.time.Duration;

public interface TowardsStation<T extends StationState> extends Towards<T> {
    T fromNeighbour(StationState stationState, GraphNode next, Duration cost, JourneyStateUpdate journeyState, GraphTransactionNeo4J txn);
    T fromStart(NotStartedState notStartedState, GraphNode firstNode, Duration cost, JourneyStateUpdate journeyState, GraphTransactionNeo4J txn);
    T fromWalking(WalkingState walkingState, GraphNode node, Duration cost, JourneyStateUpdate journeyState, GraphTransactionNeo4J txn);
    T fromGrouped(GroupedStationState groupedStationState, GraphNode next, Duration cost, JourneyStateUpdate journeyState, GraphTransactionNeo4J txn);
}
