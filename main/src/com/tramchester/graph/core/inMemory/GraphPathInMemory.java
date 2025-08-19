package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class GraphPathInMemory implements GraphPath {

    private final List<GraphEntity> entityList;
    private GraphNode lastAddedNode;
    private GraphRelationship lastAddedRelationship;

    public GraphPathInMemory() {
        entityList = new ArrayList<>();
    }

    public GraphPathInMemory(final GraphPathInMemory original) {
        entityList = new ArrayList<>(original.entityList);
        // TODO require start node for constructor so we can add this check back
//        if (original.lastAddedNode==null) {
//            throw new RuntimeException("Cannot duplicate a a path if never added a node");
//        }
        lastAddedNode = original.lastAddedNode;
        lastAddedRelationship = original.lastAddedRelationship;
    }

    public GraphPathInMemory addNode(GraphTransaction txn, GraphNode node) {
        synchronized (entityList) {
            lastAddedNode = node;
            entityList.add(node);
        }
        return this;
    }

    public GraphPathInMemory addRelationship(GraphTransaction txn, GraphRelationship graphRelationship) {
        synchronized (entityList) {
            lastAddedRelationship = graphRelationship;
            entityList.add(graphRelationship);
        }
        return this;
    }

    @Override
    public int length() {
        return entityList.size();
    }

    @Override
    public Iterable<GraphEntity> getEntities(GraphTransaction txn) {
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<GraphEntity> iterator() {
                return entityList.iterator();
            }
        };
    }

    @Override
    public GraphNode getStartNode(final GraphTransaction txn) {
        final Optional<GraphEntity> found = entityList.stream().filter(GraphEntity::isNode).findFirst();
        if (found.isEmpty()) {
            throw new RuntimeException("Could not find a start node");
        }
        return (GraphNode) found.get();
    }

    @Override
    public GraphNode getEndNode(final GraphTransaction txn) {
        synchronized (entityList) {
            return lastAddedNode;
        }
    }

    @Override
    public Iterable<GraphNode> getNodes(final GraphTransaction txn) {
        Stream<GraphNode> stream = entityList.stream().
                filter(GraphEntity::isNode).
                map(item -> (GraphNode)item);
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<GraphNode> iterator() {
                return stream.iterator();
            }
        };
    }

    @Override
    public GraphRelationship getLastRelationship(GraphTransaction txn) {
        synchronized (entityList) {
            return lastAddedRelationship;
        }
    }

    @Override
    public GraphNodeId getPreviousNodeId(final GraphTransaction txn) {
        final GraphRelationship lastRelationship = getLastRelationship(txn);
        if (lastRelationship==null) {
            return null;
        }
        return lastRelationship.getStartNodeId(txn);
    }

    public GraphPathInMemory duplicateWith(GraphTransaction txn, GraphRelationship graphRelationship) {
        return duplicateThis().addRelationship(txn, graphRelationship);
    }

    private GraphPathInMemory duplicateThis() {
        return new GraphPathInMemory(this);
    }

    public GraphPathInMemory duplicateWith(GraphTransactionInMemory txn, GraphNode currentNode) {
        return duplicateThis().addNode(txn, currentNode);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphPathInMemory that = (GraphPathInMemory) o;
        return Objects.equals(entityList, that.entityList);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entityList);
    }

    @Override
    public String toString() {
        return "GraphPathInMemory{" +
                "entityList=" + entityList +
                '}';
    }
}
