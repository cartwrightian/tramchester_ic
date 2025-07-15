package com.tramchester.graph.facade;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;

import java.util.List;
import java.util.stream.Stream;

public interface GraphTransaction extends AutoCloseable {

    // external (pure)

    int getTransactionId();

    void close();

    Stream<ImmutableGraphNode> findNodes(GraphLabel graphLabel);

    ImmutableGraphNode getNodeById(GraphNodeId nodeId);

    boolean hasAnyMatching(GraphLabel label, String field, String value);

    boolean hasAnyMatching(GraphLabel graphLabel);

    <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmutableGraphNode findNode(ITEM item);

    List<ImmutableGraphRelationship> getRouteStationRelationships(RouteStation routeStation, GraphDirection direction);
    ImmutableGraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId);

    // external (neo4j)

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
