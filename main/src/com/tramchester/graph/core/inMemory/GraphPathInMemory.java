package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.EntityList;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class GraphPathInMemory implements GraphPath {

    private final EntityList entityList;
    private GraphNode lastAddedNode;
    private GraphRelationship lastAddedRelationship;

    public GraphPathInMemory() {
        entityList = new SimpleEntityList();
    }

    private GraphPathInMemory(final GraphPathInMemory original) {
        entityList = original.entityList.branchFrom();
        lastAddedNode = original.lastAddedNode;
        lastAddedRelationship = original.lastAddedRelationship;
    }

    public GraphPathInMemory duplicate() {
        return new GraphPathInMemory(this);
    }

    public GraphPathInMemory duplicateWith(final GraphTransaction txn, final GraphRelationship graphRelationship) {
        return duplicate().addRelationship(txn, graphRelationship);
    }

    @Override
    public GraphPathInMemory duplicateWith(final GraphTransaction txn, final GraphNode currentNode) {
        return duplicate().addNode(txn, currentNode);
    }

    private GraphPathInMemory addNode(final GraphTransaction txn, final GraphNode node) {
        synchronized (entityList) {
            lastAddedNode = node;
            entityList.add(node);
        }
        return this;
    }

    private GraphPathInMemory addRelationship(final GraphTransaction txn, final GraphRelationship graphRelationship) {
        synchronized (entityList) {
            if (lastAddedNode==null) {
                throw new RuntimeException("No last added node for " + this + " trying to add " + graphRelationship);
            }
            if (!graphRelationship.getStartNodeId(txn).equals(lastAddedNode.getId())) {
                throw new RuntimeException("Consistency check failure, last node was " + lastAddedNode +
                        " but start does not match " + entityList);
            }

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
                return entityList.Stream().iterator();
            }
        };
    }

    @Override
    public GraphNode getStartNode(final GraphTransaction txn) {
        final Optional<GraphEntity> found = entityList.Stream().
                filter(GraphEntity::isNode).
                findFirst();
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
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<GraphNode> iterator() {
                return entityList.Stream().
                        filter(GraphEntity::isNode).
                        map(item -> (GraphNode)item).
                        iterator();
            }
        };
    }

    @Override
    public GraphRelationship getLastRelationship(final GraphTransaction txn) {
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

    @Override
    public TramDuration getTotalCost() {
        // todo accumulate cost as we go instead

        final Optional<TramDuration> total = entityList.Stream().
                filter(GraphEntity::isRelationship).
                map(item -> (GraphRelationship)item).
                map(GraphRelationship::getCost).
                reduce(TramDuration::plus);

        return total.orElse(TramDuration.ofSeconds(Integer.MAX_VALUE));
    }

    public boolean isEmpty() {
        return entityList.isEmpty();
    }

    public List<GraphId> getEntitiesIds() {
        return entityList.Stream().map(GraphEntity::getId).map(item-> (GraphId)item).toList();
    }

    private static class SimpleEntityList implements EntityList {

        private final List<GraphEntity> list;

        private SimpleEntityList() {
            list = new ArrayList<>();
        }

        private SimpleEntityList(final List<GraphEntity> list) {
            this.list = list;
        }

        @Override
        public EntityList branchFrom() {
            return new SimpleEntityList(new ArrayList<>(list));
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public void add(GraphEntity graphEntity) {
            list.add(graphEntity);
        }

        @Override
        public @NotNull Stream<GraphEntity> Stream() {
            return list.stream();
        }

        @Override
        public int size() {
            return list.size();
        }
    }

}
