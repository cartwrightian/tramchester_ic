package com.tramchester.graph.reference;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.collections.ImmutableEnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

@LazySingleton
public class GraphLabelsFactory {
    private static final Logger logger = LoggerFactory.getLogger(GraphLabelsFactory.class);

    private final ConcurrentMap<ImmutableEnumSet<GraphLabel>, GraphLabels> cache;

    public GraphLabelsFactory() {
        cache = new ConcurrentHashMap<>();
    }

    @PreDestroy
    private void stop() {
        logger.info("Cached contains " + cache.size() + " entries");
        cache.clear();
    }

    public GraphLabels getFor(final ImmutableEnumSet<GraphLabel> labels) {
        return cache.computeIfAbsent(labels, key -> GraphLabels.from(labels));
    }

    public GraphLabels appendTo(final GraphLabels original, final GraphLabel addition) {
        if (!cache.containsKey(original.contained())) {
            throw new RuntimeException(format("original %s is not present, cannot append %s", original, addition));
        }
        // NOTE not clearing out possibly now unreferenced initial from cache
        final ImmutableEnumSet<GraphLabel> originalEnum = original.contained();
        final ImmutableEnumSet<GraphLabel> updatedEnum = addition.addTo(originalEnum);
        return getFor(updatedEnum);
    }

}
