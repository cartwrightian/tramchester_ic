package com.tramchester.graph.facade;

import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.search.stateMachine.states.TraversalState;

import java.time.Duration;

public class PathMapper {

    private final GraphPath path;
    private final ImmutableGraphTransactionNeo4J txn;
    private TraversalState currentState;

    public PathMapper(GraphPath path, ImmutableGraphTransactionNeo4J txn) {
        this.path = path;
        this.txn = txn;
    }

    public void process(final TraversalState initial, final ForGraphNode forGraphNode, final ForGraphRelationship forGraphRelationship) {
        currentState = initial;
        Duration currentCost = Duration.ZERO;
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
        Duration getCostFor(final TraversalState current, GraphRelationship relationship);
    }

    public interface ForGraphNode {
        TraversalState getNextStateFrom(final TraversalState previous, GraphNode node, final Duration currentCost);
    }
}
