package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.repository.ReportsCacheStats;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.List;

@LazySingleton
public class ImmutableNodeCache implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(ImmutableNodeCache.class);

    private final Cache<GraphNodeId, ImmutableGraphNode> nodeCache;

    @Inject
    public ImmutableNodeCache() {
        nodeCache = Caffeine.newBuilder().
                recordStats().
                build();
    }

    public ImmutableGraphNode get(final GraphNodeId nodeId, final MutableGraphTransaction underlying) {
        return nodeCache.get(nodeId, k -> underlying.getNodeById(nodeId));
    }

    @PreDestroy
    public void stop() {
        stats().forEach(stat -> logger.info(stat.getKey() + " " + stat.getValue()));
        logger.info("stopped");
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return List.of(Pair.of("nodes", nodeCache.stats()));
    }
}
