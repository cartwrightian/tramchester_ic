package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
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

    GraphNodeInMemory getNodeMutable(NodeIdInMemory nodeId);
    GraphRelationshipInMemory getSingleRelationshipMutable(NodeIdInMemory id, GraphDirection direction,
                                                           TransportRelationshipTypes transportRelationshipType);

    Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(NodeIdInMemory id, GraphDirection direction);
    Stream<GraphNodeInMemory> findNodesMutable(GraphLabel graphLabel);

    // immutable
    Stream<GraphNode> findNodesImmutable(GraphLabel graphLabel);
    Stream<GraphNode> findNodesImmutable(GraphLabel label, GraphPropertyKey key, String value);
    GraphNode getNodeImmutable(NodeIdInMemory nodeId);

    Stream<GraphRelationship> findRelationships(TransportRelationshipTypes type);

    Stream<GraphRelationship> findRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction);
    GraphRelationship getRelationship(RelationshipIdInMemory graphRelationshipId);

    long getNumberOf(TransportRelationshipTypes relationshipType);

    void commit(GraphTransaction owningTransaction);
    void close(GraphTransaction owningTransaction);

    Stream<GraphNodeInMemory> getUpdatedNodes();
    Stream<GraphRelationshipInMemory> getUpdatedRelationships();

}
