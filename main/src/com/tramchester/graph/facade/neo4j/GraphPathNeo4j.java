package com.tramchester.graph.facade.neo4j;

import com.tramchester.graph.facade.GraphEntity;
import com.tramchester.graph.facade.GraphPath;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;
import java.util.Objects;


public class GraphPathNeo4j implements GraphPath {


    private final Path path;

    GraphPathNeo4j(Path path) {
        this.path = path;
    }

    // TODO into factory?
    public static GraphPath from(Path path) {
        return new GraphPathNeo4j(path);
    }

    @Override
    public int length() {
        return path.length();
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
    public ImmutableGraphNode getStartNode(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.fromStart(path);
    }

    @Override
    public ImmutableGraphNode getEndNode(final GraphTransaction txn) {
        final GraphTransactionNeo4J txnNeo4J = (GraphTransactionNeo4J) txn;
        return txnNeo4J.fromEnd(path);
    }

    @Override
    public Iterable<ImmutableGraphNode> getNodes(final GraphTransaction txn) {
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

    public Relationship lastRelationship() {
        return path.lastRelationship();
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
