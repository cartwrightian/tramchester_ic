package com.tramchester.graph.search;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;

import java.time.Duration;

public interface ImmutableJourneyState extends HasTransportMode {
    ImmutableTraversalState getTraversalState();
    TraversalStateType getTraversalStateType();
    TramTime getJourneyClock();
    int getNumberChanges();
    int getNumberWalkingConnections();
    boolean hasBegunJourney();
    int getNumberNeighbourConnections();
    boolean hasVisited(IdFor<Station> stationId);
    Duration getTotalDurationSoFar();
    boolean isOnDiversion();
    boolean alreadyDeparted(IdFor<Trip> tripId);
    GraphNodeId getNodeId();
    IdFor<Station> approxPosition();
}
