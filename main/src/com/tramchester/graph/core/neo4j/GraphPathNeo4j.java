package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;


public class GraphPathNeo4j implements GraphPath {

    private final Path path;

    GraphPathNeo4j(Path path) {
        this.path = path;
    }

    // TODO into factory?
    public static GraphPathNeo4j from(final Path path) {
        return new GraphPathNeo4j(path);
    }

    @Override
    public int length() {
        return path.length();
    }

    @Override
    public Duration getTotalCost() {
        final String key = GraphPropertyKey.COST.getText();

        Iterator<Relationship> iter = path.relationships().iterator();
        Duration total = Duration.ZERO;
        while (iter.hasNext()) {
            final Relationship relationship = iter.next();
            if (relationship.hasProperty(key)) {
                final int costSeconds = (int) relationship.getProperty(key);
                total = total.plus(Duration.ofSeconds(costSeconds));
            }
        }
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphPathNeo4j that = (GraphPathNeo4j) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }

    @Override
    public Iterable<GraphEntity> getEntities(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<GraphEntity> iterator() {
                return new PathIterator(path.iterator(), txnNeo4J);
            }
        };
    }

    @Override
    public GraphNode getStartNode(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.fromStart(path);
    }

    @Override
    public GraphNode getEndNode(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.fromEnd(path);
    }

    @Override
    public Iterable<GraphNode> getNodes(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.iter(path.nodes());
    }

    @Override
    public GraphRelationship getLastRelationship(final GraphTransaction txn) {
        final Relationship relationship = path.lastRelationship();
        if (relationship==null) {
            return null;
        }
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.wrapRelationship(relationship);
    }

    @Override
    public GraphNodeId getPreviousNodeId(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        final Relationship last = path.lastRelationship();
        if (last == null) {
            return null;
        } else {
            final Node previousNode = last.getStartNode();
            return txnNeo4J.getGraphIdFor(previousNode);
        }
    }

    @Override
    public GraphPathInMemory duplicateWith(GraphTransaction txn, GraphNode node) {
        throw new RuntimeException("Not implemented");
    }

    public GraphNodeId getEndNodeId(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        final Node node = path.endNode();
        return txnNeo4J.getGraphIdFor(node);
    }

    public Node endNode() {
        return path.endNode();
    }

    private static class PathIterator implements Iterator<GraphEntity> {

        private final Iterator<Entity> iterator;
        private final GraphTransactionNeo4J txn;

        public PathIterator(Iterator<Entity> iterator, GraphTransactionNeo4J txn) {
            this.iterator = iterator;
            this.txn = txn;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public GraphEntity next() {
            Entity entity = iterator.next();
            if (entity==null) {
                return null;
            }
            if (entity instanceof Node node) {
                return txn.wrapNode(node);
            }
            if (entity instanceof Relationship relationship) {
                return txn.wrapRelationship(relationship);
            }
            throw new RuntimeException("Unexpected entity " + entity);
        }
    }
}
