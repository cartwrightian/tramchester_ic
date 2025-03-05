package com.tramchester.graph.search;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;

import java.time.Duration;
import java.util.EnumSet;

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
    IdFor<? extends Location<?>> approxPosition();

    boolean alreadyVisited(GraphNode node, EnumSet<GraphLabel> labels);
}
