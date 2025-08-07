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
        return graph.createRelationship(relationshipType, (GraphNodeInMemory) begin, (GraphNodeInMemory) end);
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
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GraphNode getNodeById(GraphNodeId nodeId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasAnyMatching(GraphLabel label, String field, String value) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasAnyMatching(GraphLabel graphLabel) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(ITEM item) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<GraphRelationship> getRouteStationRelationships(RouteStation routeStation, GraphDirection direction) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GraphNodeId getPreviousNodeId(GraphPath graphPath) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GraphNodeId endNodeNodeId(GraphPath path) {
        throw new RuntimeException("Not implemented");
    }

    public Stream<GraphRelationshipInMemory> getRelationships(GraphNodeId id, GraphDirection direction, EnumSet<TransportRelationshipTypes> relationshipTypes) {
        Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        return relationships.filter(relationship -> relationshipTypes.contains(relationship.getType()));
    }

    public boolean hasRelationship(GraphNodeId id, GraphDirection direction, TransportRelationshipTypes transportRelationshipType) {
        final Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        return relationships.anyMatch(relationship -> relationship.getType().equals(transportRelationshipType));
    }

    public GraphRelationshipInMemory getSingleRelationship(GraphNodeId id, GraphDirection direction, TransportRelationshipTypes transportRelationshipTypes) {
        final Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        final List<GraphRelationshipInMemory> result = relationships.
                filter(relationship -> relationship.getType().equals(transportRelationshipTypes)).
                toList();
        if (result.size()==1) {
            return result.getFirst();
        }
        throw new RuntimeException("Wrong number of relationships " + result.size());

    }
}
