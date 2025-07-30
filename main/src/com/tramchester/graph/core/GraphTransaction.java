package com.tramchester.graph.core;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.List;
import java.util.stream.Stream;

public interface GraphTransaction extends AutoCloseable {

    void close();

    int getTransactionId();

    Stream<GraphNode> findNodes(GraphLabel graphLabel);

    GraphNode getNodeById(GraphNodeId nodeId);

    boolean hasAnyMatching(GraphLabel label, String field, String value);

    boolean hasAnyMatching(GraphLabel graphLabel);

    <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(ITEM item);

    List<ImmutableGraphRelationship> getRouteStationRelationships(RouteStation routeStation, GraphDirection direction);

    ImmutableGraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId);

    GraphNodeId getPreviousNodeId(GraphPath graphPath);

    GraphNodeId endNodeNodeId(GraphPath path);
}
