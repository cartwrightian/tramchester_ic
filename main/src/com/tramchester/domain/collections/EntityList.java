package com.tramchester.domain.collections;

import com.tramchester.graph.core.GraphEntity;
import com.tramchester.graph.core.GraphId;
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
