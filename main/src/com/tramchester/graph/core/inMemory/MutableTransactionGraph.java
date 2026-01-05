package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class MutableTransactionGraph implements Graph {
    private final Graph parent;
    private final GraphCore localGraph;
    private final AtomicInteger locallyCreateRelationships;

    private MutableTransactionGraph(final GraphCore parent, final GraphIdFactory graphIdFactory) {
        this.parent = parent;
        this.localGraph = new GraphCore(graphIdFactory);
        locallyCreateRelationships = new AtomicInteger(0);
    }

    @Override
    public GraphNodeInMemory createNode(EnumSet<GraphLabel> labels) {
        return localGraph.createNode(labels);
    }

    @Override
    public GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType, GraphNodeInMemory begin, GraphNodeInMemory end) {
        locallyCreateRelationships.getAndIncrement();
        return null;
    }

    @Override
    public void delete(RelationshipIdInMemory id) {

    }

    @Override
    public void delete(NodeIdInMemory id) {

    }

    @Override
    public void addLabel(NodeIdInMemory id, GraphLabel label) {

    }

    @Override
    public GraphNodeInMemory getNodeMutable(NodeIdInMemory nodeId) {
        return null;
    }

    @Override
    public GraphRelationshipInMemory getSingleRelationshipMutable(NodeIdInMemory id, GraphDirection direction, TransportRelationshipTypes transportRelationshipType) {
        return null;
    }

    @Override
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(NodeIdInMemory id, GraphDirection direction) {
        return Stream.empty();
    }

    @Override
    public Stream<GraphNodeInMemory> findNodesMutable(GraphLabel graphLabel) {
        return Stream.empty();
    }

    ///  immutable
    ///
    @Override
    public Stream<GraphNode> findNodesImmutable(final GraphLabel graphLabel) {
        final List<GraphNode> local = localGraph.findNodesImmutable(graphLabel).toList();
        final Stream<GraphNode> fromParent = parent.findNodesImmutable(graphLabel).filter(node -> !local.contains(node));
        return Stream.concat(local.stream(), fromParent);
    }

    @Override
    public Stream<GraphNode> findNodesImmutable(GraphLabel label, GraphPropertyKey key, String value) {
        final List<GraphNode> local = localGraph.findNodesImmutable(label, key, value).toList();
        final Stream<GraphNode> fromParent = parent.findNodesImmutable(label, key, value).filter(node -> !local.contains(node));
        return Stream.concat(local.stream(), fromParent);
    }

    @Override
    public GraphNode getNodeImmutable(final NodeIdInMemory nodeId) {
        if (localGraph.hasNodeId(nodeId)) {
            return localGraph.getNodeImmutable(nodeId);
        }
        return parent.getNodeImmutable(nodeId);
    }

    @Override
    public Stream<GraphRelationship> findRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction) {
        final List<GraphRelationship> local = localGraph.findRelationshipsImmutableFor(id, direction).toList();
        final Stream<GraphRelationship> fromParent = parent.findRelationshipsImmutableFor(id, direction);
        return Stream.concat(local.stream(), fromParent);
    }

    @Override
    public GraphRelationship getRelationship(final RelationshipIdInMemory graphRelationshipId) {
        if (localGraph.hasRelationshipId(graphRelationshipId)) {
            return localGraph.getRelationship(graphRelationshipId);
        }
        return parent.getRelationship(graphRelationshipId);
    }

    @Override
    public long getNumberOf(TransportRelationshipTypes relationshipType) {
        return parent.getNumberOf(relationshipType) + locallyCreateRelationships.get();
    }
}
