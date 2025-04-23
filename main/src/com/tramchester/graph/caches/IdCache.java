package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.facade.GraphId;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class IdCache<K extends GraphId, V extends CoreDomain> implements ClearGraphId<K> {
    private final Cache<K, IdFor<V>> cache;

    IdCache() {
        cache = Caffeine.newBuilder().
                expireAfterAccess(SharedNodeCache.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).
                recordStats().
                build();
    }

    public IdFor<V> get(final K key, final Function<K, IdFor<V>> fetcher) {
        return cache.get(key, fetcher);
    }

    public Pair<String, CacheStats> stats(final String name) {
        return Pair.of(name, cache.stats());
    }

    @Override
    public void remove(K id) {
        cache.invalidate(id);
    }
}
