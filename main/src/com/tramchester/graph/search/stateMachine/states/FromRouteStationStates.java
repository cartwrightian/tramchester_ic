package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;

public interface FromRouteStationStates {
    TraversalState fromRouteStationOnTrip(RouteStationStateOnTrip routeStationStateOnTrip, GraphNode node,
                                          Duration cost, JourneyStateUpdate journeyState, GraphTransaction txn);

    TraversalState fromRouteStationEndTrip(RouteStationStateEndTrip routeStationState, GraphNode node, Duration cost,
                                           JourneyStateUpdate journeyState, GraphTransaction txn);

}
