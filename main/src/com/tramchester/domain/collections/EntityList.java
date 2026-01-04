package com.tramchester.domain.collections;

import com.tramchester.graph.core.GraphEntity;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public interface EntityList {
    EntityList branchFrom();

    boolean isEmpty();

    void add(GraphEntity graphEntity);

    @NotNull Stream<GraphEntity> Stream();

    int size();
}
