package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.stream.Stream;

public interface Graph {

    // mutable
    GraphNodeInMemory createNode(EnumSet<GraphLabel> labels);

    GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType,
                                                 GraphNodeInMemory begin,
                                                 GraphNodeInMemory end);

    void delete(RelationshipIdInMemory id);
    void delete(NodeIdInMemory id);
    void addLabel(NodeIdInMemory id, GraphLabel label);
    Stream<GraphRelationshipInMemory> getRelationshipsMutableFor(NodeIdInMemory id, GraphDirection direction);
    GraphNodeInMemory getNodeMutable(NodeIdInMemory nodeId);
    Stream<GraphNodeInMemory> findNodesMutable(GraphLabel graphLabel);

    // immutable
    Stream<GraphNode> findNodesImmutable(GraphLabel graphLabel);
    Stream<GraphNode> findNodesImmutable(GraphLabel label, GraphPropertyKey key, String value);
    GraphNode getNodeImmutable(NodeIdInMemory nodeId);

    Stream<GraphRelationship> getRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction);
    GraphRelationship getRelationship(RelationshipIdInMemory graphRelationshipId);

    long getNumberOf(TransportRelationshipTypes relationshipType);

}
