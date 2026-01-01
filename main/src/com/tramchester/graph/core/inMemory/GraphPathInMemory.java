package com.tramchester.graph.core.inMemory;

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
        entityList = new EntityList();
    }

    public GraphPathInMemory(final GraphPathInMemory original) {
        entityList = new EntityList(original.entityList);
        lastAddedNode = original.lastAddedNode;
        lastAddedRelationship = original.lastAddedRelationship;
    }

    public GraphPathInMemory addNode(final GraphTransaction txn, final GraphNode node) {
        synchronized (entityList) {
            lastAddedNode = node;
            entityList.add(node);
        }
        return this;
    }

    public GraphPathInMemory addRelationship(final GraphTransaction txn, final GraphRelationship graphRelationship) {
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
                return entityList.iterator();
            }
        };
    }

    @Override
    public GraphNode getStartNode(final GraphTransaction txn) {
        return entityList.getFirstNode();
    }

    @Override
    public GraphNode getEndNode(final GraphTransaction txn) {
        synchronized (entityList) {
            return lastAddedNode;
        }
    }

    @Override
    public Iterable<GraphNode> getNodes(final GraphTransaction txn) {
        return entityList.getNodes();
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

    public GraphPathInMemory duplicateWith(final GraphTransaction txn, final GraphRelationship graphRelationship) {
        return duplicateThis().addRelationship(txn, graphRelationship);
    }

    public GraphPathInMemory duplicateThis() {
        return new GraphPathInMemory(this);
    }

    @Override
    public GraphPathInMemory duplicateWith(GraphTransaction txn, GraphNode currentNode) {
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

    @Override
    public TramDuration getTotalCost() {
        // todo accumulate cost as we go instead

        final Optional<TramDuration> total = entityList.getRelationships().
                map(GraphRelationship::getCost).
                reduce(TramDuration::plus);

        return total.orElse(TramDuration.ofSeconds(Integer.MAX_VALUE));

    }

    public boolean isEmpty() {
        return entityList.isEmpty();
    }

    public List<GraphId> getEntitiesIds() {
        return entityList.getEntitiesIds();
    }

    private static class EntityList {
        List<GraphEntity> list;

        public EntityList(final EntityList entityList) {
            list = new ArrayList<>(entityList.list);
        }

        public EntityList() {
            list = new ArrayList<>();
        }

        public boolean isEmpty() {
            return list.isEmpty();
        }

        public void add(final GraphEntity graphEntity) {
            list.add(graphEntity);
        }

        public int size() {
            return list.size();
        }

        public @NotNull Iterator<GraphEntity> iterator() {
            return list.iterator();
        }

        public GraphNode getFirstNode() {
            final Optional<GraphEntity> found = list.stream().filter(GraphEntity::isNode).findFirst();
            if (found.isEmpty()) {
                throw new RuntimeException("Could not find a start node");
            }
            return (GraphNode) found.get();
        }

        public Iterable<GraphNode> getNodes() {
            Stream<GraphNode> stream = list.stream().
                    filter(GraphEntity::isNode).
                    map(item -> (GraphNode)item);
            return new Iterable<>() {
                @Override
                public @NotNull Iterator<GraphNode> iterator() {
                    return stream.iterator();
                }
            };
        }

        public Stream<GraphRelationship> getRelationships() {
            return list.stream().
                filter(GraphEntity::isRelationship).
                map(entity -> (GraphRelationship) entity);
        }

        public List<GraphId> getEntitiesIds() {
            return list.stream().
                    map(GraphEntity::getId).
                    map(item -> (GraphId)item).
                    toList();
        }
    }
}
