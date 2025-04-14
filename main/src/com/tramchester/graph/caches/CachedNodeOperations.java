package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphRelationshipId;
import com.tramchester.metrics.CacheMetrics;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class CachedNodeOperations implements NodeContentsRepository {
    private static final Logger logger = LoggerFactory.getLogger(CachedNodeOperations.class);

    private final Cache<GraphRelationshipId, Duration> relationshipCostCache;

    private final NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository;

    @Inject
    public CachedNodeOperations(CacheMetrics cacheMetrics, NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository) {
        this.numberOfNodesAndRelationshipsRepository = numberOfNodesAndRelationshipsRepository;

        relationshipCostCache = createCache("relationshipCostCache", numberWithCosts());

        cacheMetrics.register(this::reportStats);
    }

    private Long numberWithCosts() {
        return EnumSet.allOf(TransportRelationshipTypes.class).stream().
                filter(TransportRelationshipTypes::hasCost).
                map(numberOfNodesAndRelationshipsRepository::numberOf).
                reduce(Long::sum).orElse(0L);
    }

    @SuppressWarnings("unused")
    @PreDestroy
    public void dispose() {
        logger.info("dispose");
        relationshipCostCache.invalidateAll();
    }

    @NonNull
    private <K, V> Cache<K, V> createCache(String name, long maximumSize) {
        // TODO cache expiry time into Config
        logger.info("Create " + name + " max size " + maximumSize);
        return Caffeine.newBuilder().maximumSize(maximumSize).
                expireAfterWrite(30, TimeUnit.MINUTES).
                recordStats().build();
    }

    private List<Pair<String,CacheStats>> reportStats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("relationshipCostCache",relationshipCostCache.stats()));

        return result;
    }

    @Override
    public Duration getCost(final GraphRelationship relationship) {
        final GraphRelationshipId relationshipId = relationship.getId();
        return relationshipCostCache.get(relationshipId, id ->  relationship.getCost());
//        final TransportRelationshipTypes relationshipType = relationship.getType();
//        if (TransportRelationshipTypes.hasCost(relationshipType)) {
//            final GraphRelationshipId relationshipId = relationship.getId();
//            return relationshipCostCache.get(relationshipId, id ->  relationship.getCost());
//        } else {
//            return Duration.ZERO;
//        }
    }

    @Override
    public void deleteFromCostCache(final GraphRelationship relationship) {
        final GraphRelationshipId relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
    }


}
