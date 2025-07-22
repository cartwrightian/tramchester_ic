package com.tramchester.graph.search;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.neo4j.GraphNodeId;
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
    Duration getTotalDurationSoFar();

    boolean alreadyDeparted(IdFor<Trip> tripId);
    GraphNodeId getNodeId();
    LocationId<?> approxPosition();

    boolean justBoarded();
    boolean alreadyBoarded(LocationId<?> locationId);
}
