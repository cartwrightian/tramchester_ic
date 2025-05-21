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

    int getTransactionId();

    void close();

    ImmutableGraphRelationship wrapRelationship(Relationship relationship);

    GraphNode wrapNode(Node node);

    ImmutableGraphNode getNodeById(GraphNodeId nodeId);

    Stream<ImmutableGraphNode> findNodes(GraphLabel graphLabel);

    boolean hasAnyMatching(GraphLabel label, String field, String value);

    boolean hasAnyMatching(GraphLabel graphLabel);

    <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmutableGraphNode findNode(ITEM item);

    EvaluationContext createEvaluationContext(GraphDatabaseService databaseService);

    List<ImmutableGraphRelationship> getRouteStationRelationships(RouteStation routeStation, Direction direction);

    Iterable<ImmutableGraphNode> iter(Iterable<Node> iterable);

    ImmutableGraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId);

    ImmutableGraphNode getStartNode(Relationship relationship);

    ImmutableGraphNode getEndNode(Relationship relationship);

    GraphNodeId getStartNodeId(Relationship relationship);

    GraphNodeId getEndNodeId(Relationship relationship);
    
    //    ImmutableGraphRelationship lastFrom(Path path);

    GraphNodeId getPreviousNodeId(Path path);

    ImmutableGraphNode fromStart(Path path);

    ImmutableGraphNode fromEnd(Path path);

    GraphNodeId endNodeNodeId(Path path);
}
