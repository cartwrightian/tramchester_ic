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
        logger.error("TODO commit for " + id);
        parent.onCommit(this);
    }

    @Override
    public void close() {
        logger.error("TODO close for " + id);
        parent.onClose(this);
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
        if (nodeId instanceof NodeIdInMemory nodeIdInMemory) {
            return graph.getNode(nodeIdInMemory);
        }
        throw new RuntimeException("Not defined for " + nodeId);
    }

    @Override
    public Stream<MutableGraphNode> findNodesMutable(final GraphLabel graphLabel) {
        return graph.findNodes(graphLabel).map(item -> item);
    }

    @Override
    public MutableGraphRelationship createRelationship(final MutableGraphNode begin, final MutableGraphNode end,
                                                       final TransportRelationshipTypes relationshipType) {
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
    public long numberOf(final TransportRelationshipTypes relationshipType) {
        return graph.getNumberOf(relationshipType);
    }

    @Override
    public GraphNode getNodeById(final GraphNodeId nodeId) {
        if (nodeId instanceof NodeIdInMemory nodeIdInMemory) {
            return graph.getNode(nodeIdInMemory);
        }
        throw new RuntimeException("Not defined for " + nodeId);
    }

    @Override
    public boolean hasAnyMatching(GraphLabel label, GraphPropertyKey key, String value) {
        return graph.findNodes(label).
                filter(node -> node.hasProperty(key)).
                anyMatch(node -> node.getProperty(key).equals(value));
    }

    @Override
    public boolean hasAnyMatching(GraphLabel graphLabel) {
        return graph.findNodes(graphLabel).findAny().isPresent();
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNodeInMemory findNode(final ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(ITEM item) {
        return findNode(item.getNodeLabel(), item.getProp(), item.getId().getGraphId());
    }

    private GraphNodeInMemory findNode(final GraphLabel label, final GraphPropertyKey propertyKey, final String itemId) {
        final List<GraphNodeInMemory> found = graph.findNodes(label).
                filter(node -> node.hasProperty(propertyKey)).
                filter(node -> node.getProperty(propertyKey).equals(itemId)).
                toList();
        if (found.isEmpty()) {
            logger.info("Did not match " + label + " " + propertyKey + " " + itemId);
            return null;
        }
        if (found.size()==1) {
            return found.getFirst();
        }
        final String message = "Unexpected number found " + found.size();
        logger.error(message);
        throw new GraphException(message);
    }

    @Override
    public List<GraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final GraphDirection direction,
                                                                EnumSet<TransportRelationshipTypes> relationshipTypes) {
        final  GraphNodeInMemory node = findNode(routeStation);
        if (node==null) {
            logger.info("Did not find node for " + routeStation.getId());
            return Collections.emptyList();
        }
        final Stream<GraphRelationship> results = graph.getRelationshipsFor(node.getId(), direction).
                filter(relationship -> relationshipTypes.contains(relationship.getType())).
                map(item ->item);
        return results.toList();
    }

    @Override
    public GraphRelationship getRelationshipById(final GraphRelationshipId graphRelationshipId) {
        if (graphRelationshipId instanceof RelationshipIdInMemory relationshipIdInMemory) {
            return graph.getRelationship(relationshipIdInMemory);
        }
        return null;
        //throw new RuntimeException("Not defined for " + graphRelationshipId);
    }

    Stream<GraphRelationship> getRelationships(final NodeIdInMemory id, final GraphDirection direction) {
        return graph.getRelationshipsFor(id, direction).map(item -> item);
    }

    Stream<GraphRelationshipInMemory> getRelationships(final NodeIdInMemory id, final GraphDirection direction,
                                                              final EnumSet<TransportRelationshipTypes> relationshipTypes) {
        final Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        return relationships.filter(relationship -> relationshipTypes.contains(relationship.getType()));
    }

    boolean hasRelationship(final NodeIdInMemory id, final GraphDirection direction, final TransportRelationshipTypes transportRelationshipType) {
        final Stream<GraphRelationshipInMemory> relationships = graph.getRelationshipsFor(id, direction);
        return relationships.anyMatch(relationship -> relationship.getType().equals(transportRelationshipType));
    }

    GraphRelationshipInMemory getSingleRelationship(final NodeIdInMemory id, final GraphDirection direction, final TransportRelationshipTypes transportRelationshipTypes) {
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

    void delete(final RelationshipIdInMemory id) {
        graph.delete(id);
    }

    void delete(final NodeIdInMemory id) {
        graph.delete(id);
    }

    void addLabel(final NodeIdInMemory id, final GraphLabel label) {
        graph.addLabel(id, label);
    }

}
