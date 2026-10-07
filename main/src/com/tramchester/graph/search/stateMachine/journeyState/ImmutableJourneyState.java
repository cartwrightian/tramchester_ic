package com.tramchester.graph.search.stateMachine.journeyState;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;

public interface ImmutableJourneyState extends HasTransportMode, CoreJourneyState {
    ImmutableTraversalState getTraversalState();

    TraversalStateType getTraversalStateType();
    TramTime getQueryTime();
    TramTime getFirstBoardTime();

    TramTime getJourneyClock();
    int getNumberChanges();
    int getNumberWalkingConnections();
    boolean hasBegunJourney();
    int getNumberNeighbourConnections();
    TramDuration getTotalDurationSoFar();

    boolean alreadyDeparted(IdFor<Trip> tripId);
    GraphNodeId getNodeId();
    LocationId<?> approxPosition();

    boolean duplicatedBoardingSeen();
    boolean justBoarded();

    boolean alreadyPassedStation(IdFor<Station> stationId);

}
