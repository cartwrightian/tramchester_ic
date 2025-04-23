package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
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

    private final IdCache<Trip> tripIdCache;
    private final IdCache<RouteStation> routeStationIdCache;
    private final IdCache<Station> stationIdCache;
    private final IdCache<Service> serviceIdCache;
    private final LabelsCache labelsCache;

    @Inject
    public SharedNodeCache() {
        tripIdCache = new IdCache<>();
        routeStationIdCache = new IdCache<>();
        serviceIdCache = new IdCache<>();
        stationIdCache = new IdCache<>();
        labelsCache = new LabelsCache();
    }

    @PreDestroy
    public void stop() {
        stats().forEach(stat -> logger.info(stat.getKey() + " " + stat.getValue()));
        logger.info("stopped");
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return List.of(tripIdCache.stats("tripIds"), routeStationIdCache.stats("routeStationIds"),
                stationIdCache.stats("stationIds"), serviceIdCache.stats("serviceIds"),
                labelsCache.stats("labels"));
    }

    public IdFor<Trip> getTripId(final GraphNodeId nodeId, final Function<GraphNodeId, IdFor<Trip>> fetcher) {
        return tripIdCache.get(nodeId, fetcher);
    }

    public IdFor<RouteStation> getRouteStationId(GraphNodeId nodeId, Function<GraphNodeId, IdFor<RouteStation>> fetcher) {
        return routeStationIdCache.get(nodeId, fetcher);
    }

    public IdFor<Station> getStationId(GraphNodeId nodeId, Function<GraphNodeId, IdFor<Station>> fetcher) {
        return stationIdCache.get(nodeId, fetcher);
    }

    public IdFor<Service> getServiceId(GraphNodeId nodeId, Function<GraphNodeId, IdFor<Service>> fetcher) {
        return serviceIdCache.get(nodeId, fetcher);
    }

    public boolean hasLabel(final GraphNodeId nodeId, final GraphLabel graphLabel, Function<GraphNodeId, EnumSet<GraphLabel>> fetcher) {
        return labelsCache.hasLabel(nodeId, graphLabel, fetcher);
    }

    public EnumSet<GraphLabel> getLabels(GraphNodeId nodeId, Function<GraphNodeId, EnumSet<GraphLabel>> fetcher) {
        return labelsCache.get(nodeId, fetcher);
    }

    private static class IdCache<T extends CoreDomain> {
        private final Cache<GraphNodeId, IdFor<T>> cache;

        private IdCache() {
            cache = Caffeine.newBuilder().
                    expireAfterAccess(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).
                    recordStats().
                    build();
        }

        public IdFor<T> get(final GraphNodeId nodeId, final Function<GraphNodeId, IdFor<T>> fetcher) {
            return cache.get(nodeId, fetcher);
        }

        public Pair<String,CacheStats> stats(final String name) {
            return Pair.of(name, cache.stats());
        }
    }

    private static class LabelsCache {
        private final Cache<GraphNodeId, EnumSet<GraphLabel>> cache;

        private LabelsCache() {
            cache = Caffeine.newBuilder().
                    expireAfterAccess(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).
                    recordStats().
                    build();
        }

        public Pair<String,CacheStats> stats(final String name) {
            return Pair.of(name, cache.stats());
        }

        public boolean hasLabel(final GraphNodeId nodeId, final GraphLabel graphLabel, final Function<GraphNodeId, EnumSet<GraphLabel>> fetcher) {
            final EnumSet<GraphLabel> labels = cache.get(nodeId, fetcher);
            return labels.contains(graphLabel);
        }

        public EnumSet<GraphLabel> get(final GraphNodeId nodeId, final Function<GraphNodeId, EnumSet<GraphLabel>> fetcher) {
            return cache.get(nodeId, fetcher);
        }
    }
}
