package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public interface EntityList {
    EntityList branchFrom();

    boolean isEmpty();

    void add(GraphNode graphEntity);
    void add(GraphRelationship graphRelationship);

    @NotNull Stream<GraphEntity<? extends GraphId>> stream();

    int size();

    GraphIdList getIds();

    class Spike implements EntityList {

        private final List<GraphEntity<? extends GraphId>> list;
        private final GraphIdListInMem ids;
        private Spike previous;

        public Spike(Spike previous) {
            this.previous = previous;
            list = new ArrayList<>();
            ids = new GraphIdListInMem();
        }

        @Override
        public EntityList branchFrom() {
            return new Spike(previous);
        }

        @Override
        public boolean isEmpty() {
            if (list.isEmpty()) {
                return previous.isEmpty();
            }
            return false;
        }

        @Override
        public void add(GraphNode node) {
            addEntity(node);
        }

        @Override
        public void add(GraphRelationship graphRelationship) {
            addEntity(graphRelationship);
        }

        public void addEntity(final GraphEntity<? extends GraphId> graphEntity) {
            list.add(graphEntity);
            ids.add(graphEntity.getId());
        }

        @Override
        public @NotNull Stream<GraphEntity<? extends GraphId>> stream() {
            return Stream.concat(previous.stream(), list.stream());
        }

        @Override
        public int size() {
            return list.size() + previous.size();
        }

        @Override
        public GraphIdList getIds() {
            throw new RuntimeException("Not implemented yet");
        }
    }

    class Simple implements EntityList {

        private final List<GraphEntity<? extends GraphId>> list;
        private final GraphIdListInMem ids;

        private Simple() {
            list = new ArrayList<>();
            ids = new GraphIdListInMem();
        }

        private Simple(final List<GraphEntity<? extends GraphId>> list, final GraphIdListInMem ids) {
            this.list = list;
            this.ids = ids;
        }

        public static EntityList create() {
            return new Simple();
        }

        @Override
        public EntityList branchFrom() {
            return new Simple(new ArrayList<>(list), new GraphIdListInMem(ids));
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public void add(final GraphNode node) {
            addEntity(node);
        }

        @Override
        public void add(final GraphRelationship relationship) {
            addEntity(relationship);
        }

        public void addEntity(final GraphEntity<? extends GraphId> graphEntity) {
            list.add(graphEntity);
            ids.add(graphEntity.getId());
        }

        @Override
        public @NotNull Stream<GraphEntity<? extends GraphId>> stream() {
            return list.stream();
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public GraphIdList getIds() {
            return ids;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Simple that = (Simple) o;
            return Objects.equals(ids, that.ids);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(ids);
        }

        @Override
        public String toString() {
            return "SimpleEntityList{" +
                    "list=" + list +
                    '}';
        }
    }
}
