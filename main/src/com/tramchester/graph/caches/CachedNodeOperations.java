package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.*;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphRelationshipId;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class CachedNodeOperations implements ReportsCacheStats, NodeContentsRepository {
    private static final Logger logger = LoggerFactory.getLogger(CachedNodeOperations.class);

    private final Cache<GraphRelationshipId, IdFor<Trip>> tripIdRelationshipCache;
    private final Cache<GraphNodeId, IdFor<Service>> serviceNodeCache;
    private final Cache<GraphNodeId, IdFor<Trip>> tripNodeCache;
    private final Cache<GraphNodeId, IdFor<RouteStation>> routeStationIdCache;

    private final Cache<GraphRelationshipId, Duration> relationshipCostCache;
    private final Cache<GraphNodeId, TramTime> timeNodeCache;
    private final Cache<GraphNodeId, Integer> hourNodeCahce;

    private final Cache<GraphNodeId, EnumSet<GraphLabel>> labelCache;

    private final NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository;

    @Inject
    public CachedNodeOperations(CacheMetrics cacheMetrics, NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository) {
        this.numberOfNodesAndRelationshipsRepository = numberOfNodesAndRelationshipsRepository;

        relationshipCostCache = createCache("relationshipCostCache", numberFor(TransportRelationshipTypes.haveCosts()));
        tripIdRelationshipCache = createCache("tripIdRelationshipCache", numberFor(TransportRelationshipTypes.haveTripId()));
        routeStationIdCache = createCache("routeStationIdCache", GraphLabel.ROUTE_STATION);
        timeNodeCache = createCache("timeNodeCache", GraphLabel.MINUTE);
        serviceNodeCache = createCache("serviceNodeCache", GraphLabel.SERVICE);
        tripNodeCache = createCache("tripNodeCache", GraphLabel.MINUTE);
        hourNodeCahce = createCache("hourNodeCache", GraphLabel.HOUR);

        labelCache = Caffeine.newBuilder().maximumSize(50000).
                expireAfterAccess(10, TimeUnit.MINUTES).
                initialCapacity(40000).
                recordStats().build();

        cacheMetrics.register(this);
    }

    private Long numberFor(Set<TransportRelationshipTypes> types) {
        return types.stream().
                map(numberOfNodesAndRelationshipsRepository::numberOf).
                reduce(Long::sum).orElse(0L);
    }

    @SuppressWarnings("unused")
    @PreDestroy
    public void dispose() {
        logger.info("dispose");
        relationshipCostCache.invalidateAll();
        serviceNodeCache.invalidateAll();
        tripIdRelationshipCache.invalidateAll();
        timeNodeCache.invalidateAll();
        routeStationIdCache.invalidateAll();
        labelCache.invalidateAll();
        hourNodeCahce.invalidateAll();
    }

    @NonNull
    private <K, V> Cache<K, V> createCache(String name, GraphLabel label) {
        return createCache(name, numberOfNodesAndRelationshipsRepository.numberOf(label));
    }

    @NonNull
    private <K, V> Cache<K, V> createCache(String name, long maximumSize) {
        // TODO cache expiry time into Config
        logger.info("Create " + name + " max size " + maximumSize);
        return Caffeine.newBuilder().maximumSize(maximumSize).expireAfterAccess(30, TimeUnit.MINUTES).
                recordStats().build();
    }

    public List<Pair<String,CacheStats>> stats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("relationshipCostCache",relationshipCostCache.stats()));
        result.add(Pair.of("svcIdCache", serviceNodeCache.stats()));
        result.add(Pair.of("tripIdRelationshipCache", tripIdRelationshipCache.stats()));
        result.add(Pair.of("timeNodeCache", timeNodeCache.stats()));
        result.add(Pair.of("routeStationIdCache", routeStationIdCache.stats()));
        result.add(Pair.of("labelCache", labelCache.stats()));

        return result;
    }

    public IdFor<Trip> getTripId(GraphRelationship relationship) {
        GraphRelationshipId relationshipId = relationship.getId();
        return tripIdRelationshipCache.get(relationshipId, id -> relationship.getTripId());
    }

    public TramTime getTime(GraphNode node) {
        GraphNodeId nodeId = node.getId();
        return timeNodeCache.get(nodeId, id -> node.getTime());
    }

    @Override
    public IdFor<RouteStation> getRouteStationId(GraphNode node) {
        GraphNodeId nodeId = node.getId();
        return routeStationIdCache.get(nodeId, id -> node.getRouteStationId());
    }

    public IdFor<Service> getServiceId(GraphNode node) {
        GraphNodeId nodeId = node.getId();
        return serviceNodeCache.get(nodeId, id -> node.getServiceId());
    }

    @Override
    public IdFor<Trip> getTripId(GraphNode node) {
        GraphNodeId nodeId = node.getId();
        return tripNodeCache.get(nodeId, id -> node.getTripId());
    }

    public int getHour(GraphNode node) {
        GraphNodeId nodeId = node.getId();
        return hourNodeCahce.get(nodeId, id -> GraphLabel.getHourFrom(getLabels(node)));
    }

    @Override
    public EnumSet<GraphLabel> getLabels(GraphNode node) {
        GraphNodeId nodeId = node.getId();
        return labelCache.get(nodeId, id -> node.getLabels());
    }

    @Override
    public Duration getCost(GraphRelationship relationship) {
        TransportRelationshipTypes relationshipType = relationship.getType();
        if (TransportRelationshipTypes.hasCost(relationshipType)) {
            GraphRelationshipId relationshipId = relationship.getId();
            return relationshipCostCache.get(relationshipId, id ->  relationship.getCost());
        } else {
            return Duration.ZERO;
        }
    }

    @Override
    public void deleteFromCostCache(GraphRelationship relationship) {
        GraphRelationshipId relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
    }


}
