package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.facade.GraphRelationshipId;
import com.tramchester.repository.ReportsCacheStats;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@LazySingleton
public class SharedRelationshipCache implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(SharedRelationshipCache.class);

    private final IdCache<GraphRelationshipId, Trip> tripIdCache;
    private final IdSetCache<Trip> tripIdListCache;

    private final List<ClearGraphId<GraphRelationshipId>> removers;

    @Inject
    public SharedRelationshipCache() {
        tripIdCache = new IdCache<>();
        tripIdListCache = new IdSetCache<>();

        removers = List.of(tripIdCache, tripIdListCache);
    }

    public IdFor<Trip> getTripId(GraphRelationshipId relationshipId, Function<GraphRelationshipId, IdFor<Trip>> fetcher) {
        return tripIdCache.get(relationshipId, fetcher);
    }

    @PreDestroy
    public void stop() {
        stats().forEach(stat -> logger.info(stat.getKey() + " " + stat.getValue()));
        logger.info("stopped");
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return List.of(tripIdCache.stats("tripIds"), tripIdListCache.stats("tripIdList"));
    }

    public IdSet<Trip> getTripIds(final GraphRelationshipId relationshipId, final Function<GraphRelationshipId, IdSet<Trip>> fetcher) {
        return tripIdListCache.get(relationshipId, fetcher);
    }

    public boolean hasTripIdInList(final IdFor<Trip> tripId, final GraphRelationshipId relationshipId, final Function<GraphRelationshipId, IdSet<Trip>> fetcher) {
        final IdSet<Trip> trips = tripIdListCache.get(relationshipId, fetcher);
        return trips.contains(tripId);
    }

    public void remove(final GraphRelationshipId id) {
        removers.forEach(cache -> cache.remove(id));
    }

    public InvalidatesCacheFor getInvalidatorFor(GraphRelationshipId id) {
        return new InvalidatesCacheFor(this,id);
    }

    private static class IdSetCache<T extends CoreDomain> implements ClearGraphId<GraphRelationshipId> {

        private final Cache<GraphRelationshipId, IdSet<T>> cache;

        private IdSetCache() {
            cache = Caffeine.newBuilder().
                    expireAfterAccess(SharedNodeCache.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).
                    recordStats().
                    build();
        }

        public IdSet<T> get(GraphRelationshipId relationshipId, Function<GraphRelationshipId, IdSet<T>> fetcher) {
            return cache.get(relationshipId, fetcher);
        }

        public Pair<String, CacheStats> stats(String name) {
            return Pair.of(name, cache.stats());
        }

        @Override
        public void remove(GraphRelationshipId id) {
            cache.invalidate(id);
        }

    }

    public static class InvalidatesCacheFor {
        private final SharedRelationshipCache parent;
        private final GraphRelationshipId id;

        private InvalidatesCacheFor(SharedRelationshipCache parent, GraphRelationshipId id) {
            this.parent = parent;
            this.id = id;
        }

        public void remove() {
            parent.remove(id);
        }
    }
}
