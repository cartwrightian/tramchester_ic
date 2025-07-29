package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.ReportsCacheStats;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@LazySingleton
public class SharedNodeCache implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(SharedNodeCache.class);
    public static final int CACHE_EXPIRE_SECONDS = 60;

    private final IdCache<GraphNodeId, Trip> tripIdCache;
    private final IdCache<GraphNodeId, RouteStation> routeStationIdCache;
    private final IdCache<GraphNodeId, Station> stationIdCache;
    private final IdCache<GraphNodeId, Service> serviceIdCache;
    private final SimpleCache<EnumSet<GraphLabel>>  labelsCache;
    private final SimpleCache<Integer> hourCache;
    private final SimpleCache<TramTime> tramTimeCache;

    private final List<ClearGraphId<GraphNodeId>> removers;

    @Inject
    public SharedNodeCache() {
        tripIdCache = new IdCache<>();
        routeStationIdCache = new IdCache<>();
        serviceIdCache = new IdCache<>();
        stationIdCache = new IdCache<>();
        labelsCache = new SimpleCache<>();
        hourCache = new SimpleCache<>();
        tramTimeCache = new SimpleCache<>();
        removers = List.of(tripIdCache, routeStationIdCache, serviceIdCache, serviceIdCache, labelsCache, hourCache,
                tramTimeCache);
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        stats().forEach(stat -> logger.info(stat.getKey() + " " + stat.getValue()));
        tramTimeCache.close();
        routeStationIdCache.close();
        serviceIdCache.close();
        labelsCache.close();
        hourCache.close();
        tramTimeCache.close();
        logger.info("stopped");
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return List.of(tripIdCache.stats("tripIds"), routeStationIdCache.stats("routeStationIds"),
                stationIdCache.stats("stationIds"), serviceIdCache.stats("serviceIds"),
                labelsCache.stats("labels"), hourCache.stats("hours"),
                tramTimeCache.stats("times"));
    }

    public IdFor<Trip> getTripId(final GraphNodeId nodeId, final Function<GraphNodeId, IdFor<Trip>> fetcher) {
        return tripIdCache.get(nodeId, fetcher);
    }

    public IdFor<RouteStation> getRouteStationId(final GraphNodeId nodeId, final Function<GraphNodeId, IdFor<RouteStation>> fetcher) {
        return routeStationIdCache.get(nodeId, fetcher);
    }

    public IdFor<Station> getStationId(final GraphNodeId nodeId, final Function<GraphNodeId, IdFor<Station>> fetcher) {
        return stationIdCache.get(nodeId, fetcher);
    }

    public IdFor<Service> getServiceId(final GraphNodeId nodeId, final Function<GraphNodeId, IdFor<Service>> fetcher) {
        return serviceIdCache.get(nodeId, fetcher);
    }

    public boolean hasLabel(final GraphNodeId nodeId, final GraphLabel graphLabel, final Function<GraphNodeId, EnumSet<GraphLabel>> fetcher) {
        final EnumSet<GraphLabel> labels = labelsCache.get(nodeId, fetcher);
        return labels.contains(graphLabel);
    }

    public EnumSet<GraphLabel> getLabels(final GraphNodeId nodeId, final Function<GraphNodeId, EnumSet<GraphLabel>> fetcher) {
        return labelsCache.get(nodeId, fetcher);
    }

    public void remove(final GraphNodeId graphNodeId) {
        removers.forEach(cache -> cache.remove(graphNodeId));
    }

    public int getHour(final GraphNodeId nodeId, final Function<GraphNodeId, Integer> fetcher) {
        return hourCache.get(nodeId, fetcher);
    }

    public TramTime getTime(final GraphNodeId nodeId, final Function<GraphNodeId, TramTime> fetcher) {
        return tramTimeCache.get(nodeId, fetcher);
    }

    public InvalidatesCacheForNode invalidatorFor(final GraphNodeId nodeId) {
        return new InvalidatesCacheForNode(nodeId, this);
    }

    private static class SimpleCache<T> implements ClearGraphId<GraphNodeId> {
        private final Cache<GraphNodeId, T> cache;

        private SimpleCache() {
            cache = Caffeine.newBuilder().
                    expireAfterAccess(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).
                    recordStats().
                    build();
        }

        public T get(final GraphNodeId nodeId, final Function<GraphNodeId, T> fetcher) {
            return cache.get(nodeId, fetcher);
        }

        @Override
        public void remove(final GraphNodeId id) {
            cache.invalidate(id);
        }

        @Override
        public void close() {
            cache.invalidateAll();
            cache.cleanUp();
        }

        public Pair<String, CacheStats> stats(final String name) {
            return Pair.of(name, cache.stats());
        }
    }

    public static class InvalidatesCacheForNode {

        private final GraphNodeId nodeId;
        private final SharedNodeCache parent;

        public InvalidatesCacheForNode(GraphNodeId nodeId, SharedNodeCache parent) {
            this.nodeId = nodeId;
            this.parent = parent;
        }

        public void remove() {
            parent.remove(nodeId);
        }
    }
}
