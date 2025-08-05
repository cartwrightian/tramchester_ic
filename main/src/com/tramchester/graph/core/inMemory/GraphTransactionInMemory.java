package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public class GraphTransactionInMemory implements MutableGraphTransaction {
    private final int id;
    private final TransactionObserver parent;
    private final Instant createdAt;
    private final Graph graph;

    public GraphTransactionInMemory(int id, TransactionObserver parent, Instant createdAt, Graph graph) {
        this.id = id;
        this.parent = parent;
        this.createdAt = createdAt;
        this.graph = graph;
    }

    @Override
    public void commit() {
        // TODO
    }

    @Override
    public MutableGraphNode createNode(final GraphLabel graphLabel) {
        return createNode(EnumSet.of(graphLabel));
    }

    @Override
    public MutableGraphNode createNode(final EnumSet<GraphLabel> labels) {
        return graph.createNode(labels);
    }

    @Override
    public MutableGraphNode getNodeByIdMutable(GraphNodeId nodeId) {
        return null;
    }

    @Override
    public Stream<MutableGraphNode> findNodesMutable(GraphLabel graphLabel) {
        return Stream.empty();
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(ITEM item) {
        return null;
    }

    @Override
    public GraphTransaction asImmutable() {
        return null;
    }

    @Override
    public MutableGraphRelationship createRelationship(MutableGraphNode begin, MutableGraphNode end, TransportRelationshipTypes relationshipType) {
        return graph.createRelationship(relationshipType, begin, end);
    }

    @Override
    public void close() {

    }

    @Override
    public int getTransactionId() {
        return id;
    }

    @Override
    public Stream<GraphNode> findNodes(GraphLabel graphLabel) {
        return Stream.empty();
    }

    @Override
    public GraphNode getNodeById(GraphNodeId nodeId) {
        return null;
    }

    @Override
    public boolean hasAnyMatching(GraphLabel label, String field, String value) {
        return false;
    }

    @Override
    public boolean hasAnyMatching(GraphLabel graphLabel) {
        return false;
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(ITEM item) {
        return null;
    }

    @Override
    public List<ImmutableGraphRelationship> getRouteStationRelationships(RouteStation routeStation, GraphDirection direction) {
        return List.of();
    }

    @Override
    public ImmutableGraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId) {
        return null;
    }

    @Override
    public GraphNodeId getPreviousNodeId(GraphPath graphPath) {
        return null;
    }

    @Override
    public GraphNodeId endNodeNodeId(GraphPath path) {
        return null;
    }
}
