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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public class GraphTransactionInMemory implements MutableGraphTransaction {
    private static final Logger logger = LoggerFactory.getLogger(GraphTransactionInMemory.class);

    private final int id;
    private final TransactionObserver parent;
    private final Graph graph;
    private final boolean immutable;

    public GraphTransactionInMemory(int id, TransactionObserver parent, Graph graph, boolean immutable) {
        this.id = id;
        this.parent = parent;
        this.graph = graph;
        this.immutable = immutable;
    }

    @Override
    public void commit() {
        if (immutable) {
            throw new RuntimeException("Immutable transaction");
        }
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
            return graph.getNodeMutable(nodeIdInMemory);
        }
        throw new RuntimeException("Not defined for " + nodeId);
    }

    @Override
    public Stream<MutableGraphNode> findNodesMutable(final GraphLabel graphLabel) {
        return graph.findNodesMutable(graphLabel).map(item -> item);
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
        return graph.findNodesImmutable(graphLabel);
    }

    @Override
    public long numberOf(final TransportRelationshipTypes relationshipType) {
        return graph.getNumberOf(relationshipType);
    }

    @Override
    public GraphNode getNodeById(final GraphNodeId nodeId) {
        if (nodeId instanceof NodeIdInMemory nodeIdInMemory) {
            return graph.getNodeImmutable(nodeIdInMemory);
        }
        throw new RuntimeException("Not defined for " + nodeId);
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel label, final GraphPropertyKey key, final String value) {
        return graph.findNodesImmutable(label, key, value).findAny().isPresent();
    }

    @Override
    public boolean hasAnyMatching(GraphLabel graphLabel) {
        return graph.findNodesImmutable(graphLabel).findAny().isPresent();
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(final ITEM item) {
        final List<GraphNode> found = graph.
                findNodesImmutable(item.getNodeLabel(), item.getProp(), item.getId().getGraphId()).toList();
        if (found.isEmpty()) {
            logger.info("Did not match " + item);
            return null;
        }
        if (found.size()==1) {
            return found.getFirst();
        }
        final String message = "Unexpected number found " + found.size() + " while seraching for " + item;
        logger.error(message);
        throw new GraphException(message);
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> MutableGraphNode findNodeMutable(ITEM item) {
        final GraphLabel label = item.getNodeLabel();
        final GraphPropertyKey propertyKey = item.getProp();
        final String itemId = item.getId().getGraphId();
        final List<GraphNodeInMemory> found = graph.findNodesMutable(label).
                filter(node -> node.hasProperty(propertyKey)).
                filter(node -> node.getProperty(propertyKey).equals(itemId)).
                toList();
        if (found.isEmpty()) {
            logger.info("Did not find " + label + " " + propertyKey + " " + itemId);
            return null;
        }
        if (found.size()==1) {
            return found.getFirst();
        }
        final String message = "Unexpected number found " + found.size() + " searching for " + item;
        logger.error(message);
        throw new GraphException(message);
    }

    @Override
    public List<GraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final GraphDirection direction,
                                                                EnumSet<TransportRelationshipTypes> relationshipTypes) {
        final GraphNode node = findNode(routeStation);
        if (node==null) {
            logger.info("Did not find node for " + routeStation.getId());
            return Collections.emptyList();
        }
        // TODO
        NodeIdInMemory idInMemory = (NodeIdInMemory) node.getId();
        final Stream<GraphRelationship> results = graph.findRelationshipsImmutableFor(idInMemory, direction).
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
        return graph.findRelationshipsImmutableFor(id, direction);
    }

    Stream<GraphRelationshipInMemory> getRelationshipMutable(final NodeIdInMemory id, final GraphDirection direction,
                                                              final EnumSet<TransportRelationshipTypes> relationshipTypes) {
        return graph.findRelationshipsMutableFor(id, direction)
                .filter(relationship -> relationshipTypes.contains(relationship.getType()));
    }

    public GraphRelationship getSingleRelationshipImmutable(NodeIdInMemory id, GraphDirection direction, TransportRelationshipTypes transportRelationshipTypes) {
        final List<GraphRelationship> result = graph.findRelationshipsImmutableFor(id, direction).
                filter(rel -> rel.isType(transportRelationshipTypes)).toList();
        if (result.size()==1) {
            return result.getFirst();
        }
        String msg = "Wrong number of relationships " + result.size();
        logger.error(msg);
        throw new GraphException(msg);
    }

    GraphRelationshipInMemory getSingleRelationshipMutable(final NodeIdInMemory id, final GraphDirection direction,
                                                    final TransportRelationshipTypes transportRelationshipType) {

        return graph.getSingleRelationshipMutable(id, direction, transportRelationshipType);
    }

    Stream<GraphRelationship> getRelationshipImmutable(final NodeIdInMemory id, final GraphDirection direction,
                                                             final EnumSet<TransportRelationshipTypes> relationshipTypes) {
        final Stream<GraphRelationship> relationships = graph.findRelationshipsImmutableFor(id, direction);
        return relationships.filter(relationship -> relationshipTypes.contains(relationship.getType()));
    }

    boolean hasRelationship(final NodeIdInMemory id, final GraphDirection direction, final TransportRelationshipTypes transportRelationshipType) {
        final Stream<GraphRelationship> relationships = graph.findRelationshipsImmutableFor(id, direction);
        return relationships.anyMatch(relationship -> relationship.getType().equals(transportRelationshipType));
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

    @Override
    public String toString() {
        return "GraphTransactionInMemory{" +
                "id=" + id +
                ", immutable=" + immutable +
                '}';
    }
}
