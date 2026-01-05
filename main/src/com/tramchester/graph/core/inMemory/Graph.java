package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.stream.Stream;

public interface Graph {
    GraphNodeInMemory createNode(EnumSet<GraphLabel> labels);

    GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType,
                                                 GraphNodeInMemory begin,
                                                 GraphNodeInMemory end);

    Stream<GraphRelationshipInMemory> getRelationshipsFor(NodeIdInMemory id, GraphDirection direction);

    void delete(RelationshipIdInMemory id);

    void delete(NodeIdInMemory id);

    GraphNodeInMemory getNode(NodeIdInMemory nodeId);

    Stream<GraphNodeInMemory> findNodes(GraphLabel graphLabel);

    GraphRelationship getRelationship(RelationshipIdInMemory graphRelationshipId);

    void addLabel(NodeIdInMemory id, GraphLabel label);

    long getNumberOf(TransportRelationshipTypes relationshipType);
}
