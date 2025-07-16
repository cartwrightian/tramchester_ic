package com.tramchester.graph.facade;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.facade.neo4j.GraphNodeId;
import com.tramchester.graph.facade.neo4j.GraphRelationshipId;
import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.List;
import java.util.stream.Stream;

public interface GraphTransaction extends AutoCloseable {
    int getTransactionId();

    void close();

    Stream<ImmutableGraphNode> findNodes(GraphLabel graphLabel);

    ImmutableGraphNode getNodeById(GraphNodeId nodeId);

    boolean hasAnyMatching(GraphLabel label, String field, String value);

    boolean hasAnyMatching(GraphLabel graphLabel);

    <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmutableGraphNode findNode(ITEM item);

    List<ImmutableGraphRelationship> getRouteStationRelationships(RouteStation routeStation, GraphDirection direction);

    ImmutableGraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId);
}
