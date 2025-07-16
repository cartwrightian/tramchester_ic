package com.tramchester.graph.facade.neo4j;

import com.tramchester.graph.facade.*;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;


public class GraphPathNeo4j implements GraphPath {
    private final Path path;

    public GraphPathNeo4j(Path path) {
        this.path = path;
    }

    @Override
    public int length() {
        return path.length();
    }

    @Override
    public Iterable<GraphEntity> getEntities(final GraphTransactionNeo4J txn) {
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<GraphEntity> iterator() {
                return new PathIterator(path.iterator(), txn);
            }
        };
    }

    @Override
    public ImmutableGraphNode getStartNode(final GraphTransactionNeo4J txn) {
        return txn.fromStart(path);
    }

    @Override
    public ImmutableGraphNode getEndNode(final GraphTransactionNeo4J txn) {
        return txn.fromEnd(path);
    }

    @Override
    public Iterable<ImmutableGraphNode> getNodes(final GraphTransactionNeo4J txn) {
        return txn.iter(path.nodes());
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
