package com.tramchester.graph.facade.neo4j;

import com.tramchester.graph.facade.GraphNode;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;

public interface GraphTransactionNeo4J extends AutoCloseable {

    ImmutableGraphRelationship wrapRelationship(Relationship relationship);

    GraphNode wrapNode(Node node);

    EvaluationContext createEvaluationContext(GraphDatabaseService databaseService);

    Iterable<ImmutableGraphNode> iter(Iterable<Node> iterable);

    GraphNodeId getPreviousNodeId(Path path);

    ImmutableGraphNode fromStart(Path path);

    ImmutableGraphNode fromEnd(Path path);

    GraphNodeId endNodeNodeId(Path path);

    // internal

    ImmutableGraphNode getStartNode(Relationship relationship);

    ImmutableGraphNode getEndNode(Relationship relationship);

    GraphNodeId getStartNodeId(Relationship relationship);

    GraphNodeId getEndNodeId(Relationship relationship);


}
