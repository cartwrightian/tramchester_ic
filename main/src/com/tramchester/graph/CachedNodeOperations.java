package com.tramchester.graph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class CachedNodeOperations implements ReportsCacheStats, NodeContentsRepository {
    private static final Logger logger = LoggerFactory.getLogger(CachedNodeOperations.class);

    private final Cache<Long, Integer> relationshipCostCache;
    private final Cache<Long, IdFor<Trip>> tripIdRelationshipCache;

    private final Cache<Long, IdFor<Service>> svcIdCache;
    private final Cache<Long, TramTime> times;
    private final HourNodeCache prebuildHourCache;

    @Inject
    public CachedNodeOperations(CacheMetrics cacheMetrics, HourNodeCache prebuildHourCache) {
        this.prebuildHourCache = prebuildHourCache;

        // size tuned from stats
        relationshipCostCache = createCache(85000);
        svcIdCache = createCache(425350);
        tripIdRelationshipCache = createCache(32500);
        times = createCache(40000);

        cacheMetrics.register(this);
    }

    @PreDestroy
    public void dispose() {
        logger.info("dispose");
        relationshipCostCache.invalidateAll();
        svcIdCache.invalidateAll();
        tripIdRelationshipCache.invalidateAll();
        times.invalidateAll();
    }

    @NonNull
    private <T> Cache<Long, T> createCache(int maximumSize) {
        // TODO cache expiry time into Config
        return Caffeine.newBuilder().maximumSize(maximumSize).expireAfterAccess(30, TimeUnit.MINUTES).
                recordStats().build();
    }

    public List<Pair<String,CacheStats>> stats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("relationshipCostCache",relationshipCostCache.stats()));
        result.add(Pair.of("svcIdCache",svcIdCache.stats()));
        result.add(Pair.of("tripIdRelationshipCache", tripIdRelationshipCache.stats()));
        result.add(Pair.of("times", times.stats()));

        return result;
    }

    public IdFor<Trip> getTrip(Relationship relationship) {
        long relationshipId = relationship.getId();
        return tripIdRelationshipCache.get(relationshipId, id -> GraphProps.getTripId(relationship));
    }

    public TramTime getTime(Node node) {
        long nodeId = node.getId();
        return times.get(nodeId, id -> GraphProps.getTime(node));
    }

    public IdFor<Service> getServiceId(Node node) {
        long nodeId = node.getId();
        return svcIdCache.get(nodeId, id -> GraphProps.getServiceId(node));
    }

    public int getHour(Node node) {
        long nodeId = node.getId();
        return prebuildHourCache.getHourFor(nodeId);
    }

    public int getCost(Relationship relationship) {
        long relationshipId = relationship.getId();
        //noinspection ConstantConditions
        return relationshipCostCache.get(relationshipId, id ->  GraphProps.getCost(relationship));
    }

    public void deleteFromCostCache(Relationship relationship) {
        long relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
    }

}
