package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphPathInMemory implements GraphPath {

    private final List<GraphEntity> entityList;
    private GraphNode lastAddedNode;
    private GraphRelationship lastAddedRelationship;

    public GraphPathInMemory() {
        entityList = new ArrayList<>();
    }

    public void addNode(GraphTransaction txn, GraphNode node) {
        synchronized (entityList) {
            lastAddedNode = node;
            entityList.add(node);
        }
    }

    public void addRelationship(GraphTransaction txn, GraphRelationship graphRelationship) {
        synchronized (entityList) {
            lastAddedRelationship = graphRelationship;
            entityList.add(graphRelationship);
        }
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
            throw new RuntimeException("Could not find a node");
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
        return getLastRelationship(txn).getStartNodeId(txn);
    }
}
