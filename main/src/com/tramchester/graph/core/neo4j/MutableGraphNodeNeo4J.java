package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Stream;

public class MutableGraphNodeNeo4J extends GraphNodeProperties<GraphPropsNeo4J> implements MutableGraphNode, CreateGraphTraverser {
    private final Node node;
    private final GraphNodeId graphNodeId;
    private final GraphReferenceMapper relationshipTypeFactory;
    private final SharedNodeCache.InvalidatesCacheForNode invalidatesCacheForNode;

    MutableGraphNodeNeo4J(Node node, GraphNodeId graphNodeId, GraphReferenceMapper relationshipTypeFactory, SharedNodeCache.InvalidatesCacheForNode invalidatesCacheForNode) {
        super(GraphPropsNeo4J.wrap(node));
        this.relationshipTypeFactory = relationshipTypeFactory;
        this.invalidatesCacheForNode = invalidatesCacheForNode;
        if (node == null) {
            throw new RuntimeException("Null node passed");
        }
        this.node = node;
        this.graphNodeId = graphNodeId;
    }

    public GraphNodeId getId() {
        return graphNodeId;
    }

    Node getNode() {
        return node;
    }

    @Override
    public void delete(MutableGraphTransaction txn) {
        invalidateCache();
        node.delete();
    }

    @Override
    public void invalidateCache() {
        invalidatesCacheForNode.remove();
    }

    @Override
    public MutableGraphRelationship createRelationshipTo(final MutableGraphTransaction txn, final MutableGraphNode end,
                                                         final TransportRelationshipTypes relationshipType) {

        // TODO address casting, here and elsewhere
        final MutableGraphTransactionNeo4J txnNeo4J = (MutableGraphTransactionNeo4J) txn;
        final MutableGraphNodeNeo4J endNode = (MutableGraphNodeNeo4J) end;

        final Relationship relationshipTo = node.createRelationshipTo(endNode.node, relationshipTypeFactory.get(relationshipType));
        return txnNeo4J.wrapRelationshipMutable(relationshipTo);
    }

    @Override
    public EnumSet<GraphLabel> getLabels() {
        return GraphReferenceMapper.from(node.getLabels());
    }

    @Override
    public void addLabel(final MutableGraphTransaction txn, final GraphLabel graphLabel) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        node.addLabel(label);
        invalidateCache();
    }

    @Override
    public Stream<GraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                      final TransportRelationshipTypes relationshipType) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return node.getRelationships(map(direction), relationshipTypeFactory.get(relationshipType)).
                stream().
                map(txnNeo4J::wrapRelationship);
    }

    private Direction map(final GraphDirection direction) {
        return switch (direction) {
            case Outgoing -> Direction.OUTGOING;
            case Incoming -> Direction.INCOMING;
            case Both -> Direction.BOTH;
        };
    }

    @Override
    public Stream<MutableGraphRelationship> getRelationshipsMutable(final MutableGraphTransaction txn, final GraphDirection direction,
                                                                    final TransportRelationshipTypes relationshipType) {
        MutableGraphTransactionNeo4J txnNeo4J = (MutableGraphTransactionNeo4J) txn;
        return node.getRelationships(map(direction), relationshipTypeFactory.get(relationshipType)).stream().map(txnNeo4J::wrapRelationshipMutable);
    }

    @Override
    public Stream<GraphRelationship> getRelationships(final GraphTransaction txn, final GraphDirection direction,
                                                      final TransportRelationshipTypes... transportRelationshipTypes) {
        GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        RelationshipType[] relationshipTypes =  relationshipTypeFactory.get(transportRelationshipTypes);
        if (relationshipTypes.length==0) {
            return node.getRelationships(map(direction)).stream().map(txnNeo4J::wrapRelationship);
        } else {
            return node.getRelationships(map(direction), relationshipTypes).stream().map(txnNeo4J::wrapRelationship);
        }
    }

    @Override
    public Stream<GraphRelationship> getRelationships(GraphTransaction txn, GraphDirection direction,
                                                      EnumSet<TransportRelationshipTypes> types) {
        if (types.isEmpty()) {
            throw new RuntimeException("Empty set of types");
        }
        final RelationshipType[] relationshipTypes =  relationshipTypeFactory.get(types);
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return node.getRelationships(map(direction), relationshipTypes).stream().map(txnNeo4J::wrapRelationship);
    }

    @Override
    public boolean hasRelationship(final GraphTransaction txn, final GraphDirection direction, final TransportRelationshipTypes transportRelationshipTypes) {
        return node.hasRelationship(map(direction), relationshipTypeFactory.get(transportRelationshipTypes));
    }

    @Override
    public boolean hasLabel(final GraphLabel graphLabel) {
        final Label label = relationshipTypeFactory.get(graphLabel);
        return node.hasLabel(label);
    }

    @Override
    public ImmutableGraphRelationshipNeo4J getSingleRelationship(GraphTransaction txn, TransportRelationshipTypes transportRelationshipType,
                                                                 GraphDirection direction) {
        final Relationship found = node.getSingleRelationship(relationshipTypeFactory.get(transportRelationshipType), map(direction));
        if (found==null) {
            return null;
        }
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.wrapRelationship(found);
    }

    @Override
    public MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction txn, TransportRelationshipTypes transportRelationshipType,
                                                                 GraphDirection direction) {
        final Relationship found = node.getSingleRelationship(relationshipTypeFactory.get(transportRelationshipType), map(direction));
        if (found==null) {
            return null;
        }
        final MutableGraphTransactionNeo4J txnNeo4J = (MutableGraphTransactionNeo4J) txn;
        return txnNeo4J.wrapRelationshipMutable(found);
    }

    ///// utility ////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "MutableGraphNode{" +
                "node=" + node +
                ", graphNodeId=" + graphNodeId +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableGraphNodeNeo4J graphNode = (MutableGraphNodeNeo4J) o;
        return Objects.equals(graphNodeId, graphNode.graphNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphNodeId);
    }

    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isRelationship() {
        return false;
    }

    @Override
    public Traverser getTraverser(TraversalDescription traversalDesc) {
        return traversalDesc.traverse(node);
    }

}
