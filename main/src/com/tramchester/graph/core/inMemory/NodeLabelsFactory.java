package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.annotations.Cached;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.reference.GraphLabel;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

@LazySingleton
public class NodeLabelsFactory {

    private final ConcurrentMap<ImmutableEnumSet<GraphLabel>, ImmutableEnumSet<GraphLabel>> cache;

    public NodeLabelsFactory() {
        cache = new ConcurrentHashMap<>();
    }

    @PreDestroy
    private void stop() {
        cache.clear();
    }

    public ImmutableEnumSet<GraphLabel> getFor(final ImmutableEnumSet<GraphLabel> initial) {
        return cache.computeIfAbsent(initial, key -> initial);
    }

    public ImmutableEnumSet<GraphLabel> appendTo(@Cached final ImmutableEnumSet<GraphLabel> original, final GraphLabel addition) {
        if (!cache.containsKey(original)) {
            throw new RuntimeException(format("original %s is not present, cannot append %s", original, addition));
        }
        // NOTE not clearing out possibly now unreferenced initial from cache
        final ImmutableEnumSet<GraphLabel> toAdd = addition.addTo(original);
        return getFor(toAdd);
    }

}
