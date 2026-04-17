package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.stream.Stream;

public interface Graph extends ImmutableGraph {

    // transaction handling
    void commit(GraphTransaction owningTransaction);
    void close(GraphTransaction owningTransaction);
    Stream<GraphNodeInMemory> getUpdatedNodes();
    Stream<GraphRelationshipInMemory> getUpdatedRelationships();

    // mutable
    GraphNodeInMemory createNode(ImmutableEnumSet<GraphLabel> labels);

    GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType,
                                                 GraphNodeInMemory begin,
                                                 GraphNodeInMemory end);

    void delete(RelationshipIdInMemory id);
    void delete(NodeIdInMemory id);
    void addLabel(NodeIdInMemory id, GraphLabel label);

    GraphNodeInMemory getNodeMutable(NodeIdInMemory nodeId);


    Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(NodeIdInMemory id, GraphDirection direction,
                                                                  ImmutableEnumSet<TransportRelationshipTypes> types);

    GraphRelationshipInMemory getSingleRelationshipMutable(NodeIdInMemory id, GraphDirection direction,
                                                           TransportRelationshipTypes transportRelationshipType);

    Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(NodeIdInMemory id, GraphDirection direction);
    Stream<GraphNodeInMemory> findNodesMutable(GraphLabel graphLabel);

    boolean isImmutable();

    /***
     * primarily for test/analysis support
     * @return all nodes in the DB
     */
    Stream<GraphNode> allNodes();
}
