package com.tramchester.graph.facade;

import com.tramchester.graph.search.stateMachine.states.TraversalState;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;

public class PathMapper {

    private final Path path;
    private final ImmutableGraphTransactionNeo4J txn;
    private TraversalState currentState;

    public PathMapper(Path path, ImmutableGraphTransactionNeo4J txn) {
        this.path = path;
        this.txn = txn;
    }

    public void process(final TraversalState initial, final ForGraphNode forGraphNode, final ForGraphRelationship forGraphRelationship) {
        currentState = initial;
        Duration currentCost = Duration.ZERO;
        for (Entity entity : path) {
            if (entity instanceof Node node) {
                final GraphNode graphNode = txn.wrapNode(node);
                currentState = forGraphNode.getNextStateFrom(currentState, graphNode, currentCost);
            }
            if (entity instanceof Relationship relationship) {
                final GraphRelationship graphRelationship = txn.wrapRelationship(relationship);
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
