package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;

public interface FromRouteStationStates {
    TraversalState fromRouteStationOnTrip(RouteStationStateOnTrip routeStationStateOnTrip, GraphNode node,
                                          Duration cost, JourneyStateUpdate journeyState, GraphTransactionNeo4J txn);

    TraversalState fromRouteStationEndTrip(RouteStationStateEndTrip routeStationState, GraphNode node, Duration cost,
                                           JourneyStateUpdate journeyState, GraphTransactionNeo4J txn);

}
