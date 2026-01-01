package com.tramchester.graph.core;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.search.stateMachine.states.TraversalState;

public class PathMapper {

    private final GraphPath path;
    private final GraphTransaction txn;
    private TraversalState currentState;

    public PathMapper(GraphPath path, GraphTransaction txn) {
        this.path = path;
        this.txn = txn;
    }

    public void process(final TraversalState initial, final ForGraphNode forGraphNode, final ForGraphRelationship forGraphRelationship) {
        currentState = initial;
        TramDuration currentCost = TramDuration.ZERO;
        for (GraphEntity entity : path.getEntities(txn)) {
            if (entity.isNode()) {
                final GraphNode graphNode = (GraphNode) entity;
                currentState = forGraphNode.getNextStateFrom(currentState, graphNode, currentCost);
            }
            if (entity.isRelationship()) {
                final GraphRelationship graphRelationship = (GraphRelationship) entity;
                currentCost = forGraphRelationship.getCostFor(currentState, graphRelationship);
            }
        }
    }

    public TraversalState getFinalState() {
        return currentState;
    }

    public interface ForGraphRelationship {
        TramDuration getCostFor(final TraversalState current, GraphRelationship relationship);
    }

    public interface ForGraphNode {
        TraversalState getNextStateFrom(final TraversalState previous, GraphNode node, final TramDuration currentCost);
    }
}
