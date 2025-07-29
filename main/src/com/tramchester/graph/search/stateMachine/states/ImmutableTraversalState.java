package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationshipNeo4J;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;

import java.time.Duration;
import java.util.EnumSet;
import java.util.stream.Stream;

public interface ImmutableTraversalState {

    ImmutableTraversalState nextState(EnumSet<GraphLabel> nodeLabels, GraphNode node,
                             JourneyStateUpdate journeyState, Duration duration);

    Duration getTotalDuration();

    TraversalStateType getStateType();

    GraphNodeId nodeId();

    GraphTransactionNeo4J getTransaction();

    TraversalStateFactory getTraversalStateFactory();

    Stream<ImmutableGraphRelationshipNeo4J> getOutbounds();
}
