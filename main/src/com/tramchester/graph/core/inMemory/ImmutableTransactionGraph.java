package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.GraphLabels;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class ImmutableTransactionGraph implements Graph {
    private final GraphCache cache;

    public ImmutableTransactionGraph(final Graph graph) {
        this.cache = new GraphCache(graph);
    }

    @Override
    public void commit(GraphTransaction owningTransaction) {
        throw new RuntimeException("Got unexpected commit for " + owningTransaction);
    }

    @Override
    public void close(GraphTransaction owningTransaction) {
        cache.clear();
    }

    @Override
    public GraphNodeInMemory createNode(final ImmutableEnumSet<GraphLabel> labels) {
        throw new ImmutableGraphException();
    }

    @Override
    public GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType, GraphNodeInMemory begin, GraphNodeInMemory end) {
        throw new ImmutableGraphException();
    }

    @Override
    public void delete(RelationshipIdInMemory id) {
        throw new ImmutableGraphException();
    }

    @Override
    public void delete(NodeIdInMemory id) {
        throw new ImmutableGraphException();
    }

    @Override
    public void addLabel(NodeIdInMemory id, GraphLabel label) {
        throw new ImmutableGraphException();
    }

    @Override
    public GraphNodeInMemory getNodeMutable(NodeIdInMemory nodeId) {
        throw new ImmutableGraphException();
    }

    @Override
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(final NodeIdInMemory id, final GraphDirection direction, final ImmutableEnumSet<TransportRelationshipTypes> types) {
        throw new ImmutableGraphException();
    }

    @Override
    public GraphRelationshipInMemory getSingleRelationshipMutable(NodeIdInMemory id, GraphDirection direction, TransportRelationshipTypes transportRelationshipType) {
        throw new ImmutableGraphException();
    }

    @Override
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(NodeIdInMemory id, GraphDirection direction) {
        throw new ImmutableGraphException();
    }

    @Override
    public Stream<GraphNodeInMemory> findNodesMutable(GraphLabel graphLabel) {
        throw new ImmutableGraphException();
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    // immutable

    @Override
    public Stream<GraphRelationship> findRelationshipsImmutableFor(final NodeIdInMemory id, final GraphDirection direction,
                                                                   final ImmutableEnumSet<TransportRelationshipTypes> types) {
        return cache.findRelationshipsImmutableFor(id, direction, types);
    }

    @Override
    public Stream<GraphNode> findNodesImmutable(final GraphLabel graphLabel) {
        return cache.findNodesImmutable(graphLabel);
    }

    @Override
    public Stream<GraphNode> findNodesImmutable(final GraphLabel label, final GraphPropertyKey key, final String value) {
        return cache.findNodesImmutable(label, key, value);
    }

    @Override
    public Stream<GraphNode> allNodes() {
        return cache.allNodes();
    }

    @Override
    public GraphLabels updateLabels(GraphLabels original, GraphLabel addition) {
        throw new ImmutableGraphException();
    }

    @Override
    public GraphNode getNodeImmutable(final NodeIdInMemory nodeId) {
        return cache.getNodeImmutable(nodeId);
    }

    @Override
    public Stream<GraphRelationship> findRelationships(final TransportRelationshipTypes type) {
        return cache.findRelationships(type);
    }

    @Override
    public Stream<GraphRelationship> findRelationshipsImmutableFor(final NodeIdInMemory id, final GraphDirection direction) {
        return cache.findRelationshipsImmutableFor(id, direction);
    }

    @Override
    public GraphRelationship getRelationship(final RelationshipIdInMemory graphRelationshipId) {
        return cache.getRelationship(graphRelationshipId);
    }

    @Override
    public long getNumberOf(TransportRelationshipTypes relationshipType) {
        return cache.getNumberOf(relationshipType);
    }

    @Override
    public Stream<GraphNodeInMemory> getUpdatedNodes() {
        return Stream.empty();
    }

    @Override
    public Stream<GraphRelationshipInMemory> getUpdatedRelationships() {
        return Stream.empty();
    }

    private static class ImmutableGraphException extends RuntimeException {
        ImmutableGraphException() {
            super("Not implemented for ImmutableGraph");
        }
    }

    private static class GraphCache implements ImmutableGraph {

        private final Graph underlying;
        // targeted on key methods used during route calculation
        private final ConcurrentMap<Pair<NodeIdInMemory, GraphDirection>, List<GraphRelationship>> relationshipCache;

        public GraphCache(final Graph underlying) {
            this.underlying = underlying;
            relationshipCache = new ConcurrentHashMap<>();
        }

        public void clear() {
            relationshipCache.clear();
        }

        @Override
        public Stream<GraphNode> findNodesImmutable(final GraphLabel graphLabel) {
            return underlying.findNodesImmutable(graphLabel);
        }

        @Override
        public Stream<GraphNode> findNodesImmutable(GraphLabel label, GraphPropertyKey key, String value) {
            return underlying.findNodesImmutable(label, key, value);
        }

        @Override
        public GraphNode getNodeImmutable(NodeIdInMemory nodeId) {
            return underlying.getNodeImmutable(nodeId);
        }

        @Override
        public Stream<GraphRelationship> findRelationships(TransportRelationshipTypes type) {
            return underlying.findRelationships(type);
        }

        @Override
        public Stream<GraphRelationship> findRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction) {
            Pair<NodeIdInMemory, GraphDirection> key = Pair.of(id,direction);
            List<GraphRelationship> items = relationshipCache.computeIfAbsent(key, x -> underlying.findRelationshipsImmutableFor(id, direction).toList());
            return items.stream();
            //return underlying.findRelationshipsImmutableFor(id, direction);
        }

        @Override
        public Stream<GraphRelationship> findRelationshipsImmutableFor(NodeIdInMemory id, GraphDirection direction,
                                                                       ImmutableEnumSet<TransportRelationshipTypes> types) {
            Pair<NodeIdInMemory, GraphDirection> key = Pair.of(id,direction);
            List<GraphRelationship> items = relationshipCache.computeIfAbsent(key, x -> underlying.findRelationshipsImmutableFor(id, direction).toList());

            return items.stream().filter(rel -> types.contains(rel.getType()));
            //return underlying.findRelationshipsImmutableFor(id, direction, types);
        }

        @Override
        public GraphRelationship getRelationship(RelationshipIdInMemory graphRelationshipId) {
            return underlying.getRelationship(graphRelationshipId);
        }

        @Override
        public long getNumberOf(TransportRelationshipTypes relationshipType) {
            return underlying.getNumberOf(relationshipType);
        }

        /***
         * primarily for test/analysis support
         * @return all nodes in the DB
         */
        public Stream<GraphNode> allNodes() {
            return underlying.allNodes();
        }
    }
}
