package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public class GraphTransactionInMemory implements MutableGraphTransaction {
    private static final Logger logger = LoggerFactory.getLogger(GraphTransactionInMemory.class);

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
    public void close() {
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
    public MutableGraphNode getNodeByIdMutable(final GraphNodeId nodeId) {
        return graph.getNode(nodeId);
    }

    @Override
    public Stream<MutableGraphNode> findNodesMutable(final GraphLabel graphLabel) {
        return graph.findNodes(graphLabel).map(item -> item);
    }

    @Override
    public MutableGraphRelationship createRelationship(MutableGraphNode begin, MutableGraphNode end, TransportRelationshipTypes relationshipType) {
        return graph.createRelationship(relationshipType, (GraphNodeInMemory) begin, (GraphNodeInMemory) end);
    }

    @Override
    public int getTransactionId() {
        return id;
    }

    @Override
    public Stream<GraphNode> findNodes(final GraphLabel graphLabel) {
        return graph.findNodes(graphLabel).map(item -> item);
    }

    @Override
    public GraphNode getNodeById(final GraphNodeId nodeId) {
        return graph.getNode(nodeId);
    }

    @Override
    public boolean hasAnyMatching(GraphLabel label, GraphPropertyKey key, String value) {
        return graph.findNodes(label).
                filter(node -> node.hasProperty(key)).
                anyMatch(node -> node.getPropery(key).equals(value));
    }

    @Override
    public boolean hasAnyMatching(GraphLabel graphLabel) {
        return graph.findNodes(graphLabel).findAny().isPresent();
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(final ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    private GraphNodeInMemory findNode(final GraphLabel label, final GraphPropertyKey propertyKey, final String itemId) {
        final List<GraphNodeInMemory> found = graph.findNodes(label).
                filter(node -> node.hasProperty(propertyKey)).
                filter(node -> node.getPropery(propertyKey).equals(itemId)).
                toList();
        if (found.isEmpty()) {
            logger.info("Did not match " + label + " " + propertyKey + " " + itemId);
            return null;
        }
        if (found.size()==1) {
            return found.getFirst();
        }
        final String message = "Unexpected number found " + found.getFirst();
        logger.error(message);
        throw new GraphException(message);
    }

    @Override
    public List<GraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final GraphDirection direction) {
        final GraphNode node = findNode(routeStation);
        if (node==null) {
            logger.info("Did not find node for " + routeStation.getId());
            return Collections.emptyList();
        }
        final Stream<GraphRelationship> results = graph.getRelationshipsFor(node.getId(), direction).
                map(item ->item);
        return results.toList();
    }

    @Override
    public GraphRelationship getRelationshipById(final GraphRelationshipId graphRelationshipId) {
        return graph.getRelationship(graphRelationshipId);
    }

    public Stream<GraphRelationshipInMemory> getRelationships(final GraphNodeId id, final GraphDirection direction,
                                                              final EnumSet<TransportRelationshipTypes> relationshipTypes) {
        Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        return relationships.filter(relationship -> relationshipTypes.contains(relationship.getType()));
    }

    public boolean hasRelationship(final GraphNodeId id, final GraphDirection direction, final TransportRelationshipTypes transportRelationshipType) {
        final Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        return relationships.anyMatch(relationship -> relationship.getType().equals(transportRelationshipType));
    }

    public GraphRelationshipInMemory getSingleRelationship(final GraphNodeId id, final GraphDirection direction, final TransportRelationshipTypes transportRelationshipTypes) {
        final Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        final List<GraphRelationshipInMemory> result = relationships.
                filter(relationship -> relationship.getType().equals(transportRelationshipTypes)).
                toList();
        if (result.size()==1) {
            return result.getFirst();
        }
        String msg = "Wrong number of relationships " + result.size();
        logger.error(msg);
        throw new GraphException(msg);

    }

    public void delete(final GraphRelationshipId id) {
        graph.delete(id);
    }

    public void delete(final GraphNodeId id) {
        graph.delete(id);
    }

    public void addLabel(final GraphNodeId id, final GraphLabel label) {
        graph.addLabel(id, label);
    }
}
