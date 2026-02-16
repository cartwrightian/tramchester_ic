package com.tramchester.graph.core;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public interface EntityList {
    EntityList branchFrom();

    boolean isEmpty();

    void add(GraphEntity<? extends GraphId> graphEntity);

    @NotNull Stream<GraphEntity<? extends GraphId>> Stream();

    int size();

    List<GraphId> getIds();
}
