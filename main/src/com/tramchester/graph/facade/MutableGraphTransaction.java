package com.tramchester.graph.facade;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.graph.facade.neo4j.MutableGraphNodeNeo4J;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.EnumSet;
import java.util.stream.Stream;

public interface MutableGraphTransaction extends GraphTransaction {
    MutableGraphNodeNeo4J createNode(GraphLabel graphLabel);

    MutableGraphNodeNeo4J createNode(EnumSet<GraphLabel> labels);

    MutableGraphNodeNeo4J getNodeByIdMutable(GraphNodeId nodeId);

    Stream<MutableGraphNodeNeo4J> findNodesMutable(GraphLabel graphLabel);

    <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNodeNeo4J findNodeMutable(ITEM item);

    void commit();

    GraphTransaction asImmutable();
}
