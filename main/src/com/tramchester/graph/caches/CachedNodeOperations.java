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
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphRelationshipId;
import com.tramchester.graph.graphbuild.GraphLabel;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LazySingleton
public class CachedNodeOperations implements NodeContentsRepository {
    private static final Logger logger = LoggerFactory.getLogger(CachedNodeOperations.class);

    private final Cache<GraphRelationshipId, IdFor<Trip>> tripIdRelationshipCache;
    private final Cache<GraphRelationshipId, Duration> relationshipCostCache;

    private final Cache<GraphNodeId, TramTime> timeNodeCache;

    private final Cache<GraphNodeId, EnumSet<GraphLabel>> labelCache;

    private final NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository;

    @Inject
    public CachedNodeOperations(CacheMetrics cacheMetrics, NumberOfNodesAndRelationshipsRepository numberOfNodesAndRelationshipsRepository) {
        this.numberOfNodesAndRelationshipsRepository = numberOfNodesAndRelationshipsRepository;

        relationshipCostCache = createCache("relationshipCostCache", numberFor(TransportRelationshipTypes.haveCosts()));
        tripIdRelationshipCache = createCache("tripIdRelationshipCache", numberFor(TransportRelationshipTypes.haveTripId()));
        timeNodeCache = createCache("timeNodeCache", GraphLabel.MINUTE);

        long numberOfNodes = numberOfNodesAndRelationshipsRepository.numberOfNodes();

        // expire after write gives significany perf improvement, less work to do with each get
        labelCache = Caffeine.newBuilder().
                maximumSize(numberOfNodes * 3).
                expireAfterWrite(10, TimeUnit.MINUTES).
                initialCapacity(40000).
                recordStats().build();

        cacheMetrics.register(this::reportStats);
    }

    private Long numberFor(final Set<TransportRelationshipTypes> types) {
        return types.stream().
                map(numberOfNodesAndRelationshipsRepository::numberOf).
                reduce(Long::sum).orElse(0L);
    }

    @SuppressWarnings("unused")
    @PreDestroy
    public void dispose() {
        logger.info("dispose");
        relationshipCostCache.invalidateAll();
        tripIdRelationshipCache.invalidateAll();
        timeNodeCache.invalidateAll();
        labelCache.invalidateAll();
    }

    @NonNull
    private <K, V> Cache<K, V> createCache(final String name, final GraphLabel label) {
        return createCache(name, numberOfNodesAndRelationshipsRepository.numberOf(label));
    }

    @NonNull
    private <K, V> Cache<K, V> createCache(String name, long maximumSize) {
        // TODO cache expiry time into Config
        logger.info("Create " + name + " max size " + maximumSize);
        return Caffeine.newBuilder().maximumSize(maximumSize).
                expireAfterWrite(30, TimeUnit.MINUTES).
                recordStats().build();
    }

    private final List<Pair<String,CacheStats>> reportStats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("relationshipCostCache",relationshipCostCache.stats()));
        result.add(Pair.of("tripIdRelationshipCache", tripIdRelationshipCache.stats()));
        result.add(Pair.of("timeNodeCache", timeNodeCache.stats()));
        result.add(Pair.of("labelCache", labelCache.stats()));

        return result;
    }

    public IdFor<Trip> getTripId(final GraphRelationship relationship) {
        final GraphRelationshipId relationshipId = relationship.getId();
        return tripIdRelationshipCache.get(relationshipId, id -> relationship.getTripId());
    }

    public TramTime getTime(final GraphNode node) {
        final GraphNodeId nodeId = node.getId();
        return timeNodeCache.get(nodeId, id -> node.getTime());
    }

    @Override
    public IdFor<RouteStation> getRouteStationId(final GraphNode node) {
        return node.getRouteStationId();
    }

    public IdFor<Service> getServiceId(final GraphNode node) {
        return node.getServiceId();
    }

    @Override
    public IdFor<Trip> getTripId(final GraphNode node) {
        return node.getTripId();
    }

    @Override
    public EnumSet<GraphLabel> getLabels(final GraphNode node) {
        final GraphNodeId nodeId = node.getId();
        return labelCache.get(nodeId, id -> node.getLabels());
    }

    @Override
    public Duration getCost(final GraphRelationship relationship) {
        final TransportRelationshipTypes relationshipType = relationship.getType();
        if (TransportRelationshipTypes.hasCost(relationshipType)) {
            final GraphRelationshipId relationshipId = relationship.getId();
            return relationshipCostCache.get(relationshipId, id ->  relationship.getCost());
        } else {
            return Duration.ZERO;
        }
    }

    @Override
    public void deleteFromCostCache(final GraphRelationship relationship) {
        final GraphRelationshipId relationshipId = relationship.getId();
        relationshipCostCache.invalidate(relationshipId);
    }


}
