package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.JourneyStateUpdate;

public interface FromRouteStationStates {
    TraversalState fromRouteStationOnTrip(RouteStationStateOnTrip routeStationStateOnTrip, GraphNode node,
                                          TramDuration cost, JourneyStateUpdate journeyState, GraphTransaction txn);

    TraversalState fromRouteStationEndTrip(RouteStationStateEndTrip routeStationState, GraphNode node, TramDuration cost,
                                           JourneyStateUpdate journeyState, GraphTransaction txn);

}
