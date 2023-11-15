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
    void close();

    GraphNode getNodeById(GraphNodeId nodeId);

    Stream<GraphNode> findNodes(GraphLabel graphLabel);

    boolean hasAnyMatching(GraphLabel label, String field, String value);

    boolean hasAnyMatching(GraphLabel graphLabel);

    <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(ITEM item);

    EvaluationContext createEvaluationContext(GraphDatabaseService databaseService);

    List<GraphRelationship> getRouteStationRelationships(RouteStation routeStation, Direction direction);

    Iterable<ImmuableGraphNode> iter(Iterable<Node> iterable);

    GraphRelationship wrapRelationship(Relationship relationship);

    GraphNode wrapNode(Node node);

    GraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId);

    ImmuableGraphNode fromEnd(Path path);

    GraphRelationship lastFrom(Path path);

    GraphNode fromStart(Path path);

    GraphNodeId createNodeId(Node node);
}
