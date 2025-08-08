package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;

public interface GraphTransactionNeo4J extends AutoCloseable {

    ImmutableGraphRelationshipNeo4J wrapRelationship(Relationship relationship);

    GraphNode wrapNode(Node node);

    EvaluationContext createEvaluationContext(GraphDatabaseService databaseService);

    Iterable<GraphNode> iter(Iterable<Node> iterable);

    GraphNode fromStart(Path path);

    GraphNode fromEnd(Path path);

    // internal

    GraphNode getStartNode(Relationship relationship);

    GraphNode getEndNode(Relationship relationship);

    GraphNodeId getStartNodeId(Relationship relationship);

    GraphNodeId getEndNodeId(Relationship relationship);

    GraphNodeId getGraphIdFor(Node node);
}
