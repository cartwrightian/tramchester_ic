package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.stream.Stream;

public interface ImmutableGraph {
    Stream<GraphNode> findNodesImmutable(GraphLabel graphLabel);
    Stream<GraphNode> findNodesImmutable(GraphLabel label, GraphPropertyKey key, String value);
    GraphNode getNodeImmutable(NodeIdInMemory nodeId);

    Stream<GraphRelationship> findRelationships(TransportRelationshipTypes type);
    Stream<GraphRelationship> findRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction);
    Stream<GraphRelationship> findRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction,
                                                            ImmutableEnumSet<TransportRelationshipTypes> types);
    GraphRelationship getRelationship(RelationshipIdInMemory graphRelationshipId);

    long getNumberOf(TransportRelationshipTypes relationshipType);
}
