package com.tramchester.graph.facade;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.EnumSet;
import java.util.stream.Stream;

public interface MutableGraphTransaction extends GraphTransaction {

    void commit();

    MutableGraphNode createNode(GraphLabel graphLabel);

    MutableGraphNode createNode(EnumSet<GraphLabel> labels);

    MutableGraphNode getNodeByIdMutable(GraphNodeId nodeId);

    Stream<MutableGraphNode> findNodesMutable(GraphLabel graphLabel);

    <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(ITEM item);

    GraphTransaction asImmutable();
}
