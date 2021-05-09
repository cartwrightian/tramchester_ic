package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import org.neo4j.graphdb.Node;

import java.util.Set;

public interface ImmuatableTraversalState {
    int getTotalCost();
    TraversalState nextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                             JourneyState journeyState, int cost);
}
