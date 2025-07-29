package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.diagnostics.HeuristicsReason;
import com.tramchester.graph.search.diagnostics.HeuristicsReasons;
import com.tramchester.graph.search.diagnostics.HowIGotHere;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.tramchester.graph.search.diagnostics.ReasonCode.NotOnQueryDate;

public class PreviousVisits implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(PreviousVisits.class);

    private static final int CACHE_DURATION_MINS = 5;

    private final Cache<GraphNodeId, HeuristicsReason> timeNodePrevious;
    private final Cache<NodeIdKeyWith<TramTime>, HeuristicsReason> hourNodePrevious;
    private final Cache<NodeIdKeyWith<TramTime>, HeuristicsReason> routeStationPrevious;
    private final Cache<GraphNodeId, HeuristicsReason> servicePrevious;
    private final boolean cachingDisabled;

    public PreviousVisits(boolean cachingDisabled, NumberOfNodesAndRelationshipsRepository countsNodes) {
        this.cachingDisabled = cachingDisabled;
        timeNodePrevious = createCache(countsNodes.numberOf(GraphLabel.MINUTE));
        hourNodePrevious = createCache(countsNodes.numberOf(GraphLabel.HOUR));
        routeStationPrevious = createCache(countsNodes.numberOf(GraphLabel.ROUTE_STATION));
        servicePrevious = createCache(countsNodes.numberOf(GraphLabel.SERVICE));
    }

    @NotNull
    private <KEY, VALUE> Cache<KEY, VALUE> createCache(final long maxCacheSize) {
        // expireAfterWrite have better performance when lots of gets
        return Caffeine.newBuilder().
                maximumSize(maxCacheSize).
                expireAfterWrite(CACHE_DURATION_MINS, TimeUnit.MINUTES).
                recordStats().build();
    }

    // TODO Disable for depth first?

    public void cacheVisitIfUseful(final HeuristicsReason reason, final GraphNode node, ImmutableJourneyState journeyState,
                                   final EnumSet<GraphLabel> labels) {
        if (cachingDisabled) {
            return;
        }

        final ReasonCode reasonCode = reason.getReasonCode();

        final GraphNodeId id = node.getId();

        if (labels.contains(GraphLabel.MINUTE) || labels.contains(GraphLabel.HOUR)) {
            // time and hour nodes represent the time on the actual journey, so if we have been here before
            // we will get the same result
            final TramTime journeyClock = journeyState.getJourneyClock();

            switch (reasonCode) {
                case DoesNotOperateOnTime -> timeNodePrevious.put(id, reason);
                case NotAtHour -> hourNodePrevious.put(new NodeIdKeyWith<>(id, journeyClock), reason);
            }

            return;
        }

        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            final TramTime journeyClock = journeyState.getJourneyClock();

            routeStationPrevious.put(new NodeIdKeyWith<>(id, journeyClock), reason);
            return;
        }

        if (labels.contains(GraphLabel.SERVICE)) {
            if (reasonCode == NotOnQueryDate) {
                // the service is unavailable for the query date
                final TramTime journeyClock = journeyState.getJourneyClock();
                final boolean isNextDay = journeyClock.isNextDay();
                if (!isNextDay) {
                    servicePrevious.put(id, reason);
                }
            }
        }
    }

    public HeuristicsReason getPreviousResult(final ImmutableJourneyState journeyState,
                                              final EnumSet<GraphLabel> labels, HowIGotHere howIGotHere) {

        if (cachingDisabled) {
            return HeuristicsReasons.CacheMiss(howIGotHere);
        }

        final GraphNodeId nodeId = howIGotHere.getEndNodeId();

        if (labels.contains(GraphLabel.MINUTE)) {
            // time node has by definition a unique time and can only arrive at the "same time" as previous visits
            final HeuristicsReason timeFound = timeNodePrevious.getIfPresent(nodeId);
            if (timeFound != null) {
                return timeFound;
            }
        }

        if (labels.contains(GraphLabel.HOUR)) {
            // Can arrive at a hour nodes at different times, so need to include that in the key
            final HeuristicsReason hourFound = hourNodePrevious.getIfPresent(new NodeIdKeyWith<>(nodeId, journeyState.getJourneyClock()));
            if (hourFound != null) {
                return hourFound;
            }
        }

        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            final HeuristicsReason found = routeStationPrevious.getIfPresent(new NodeIdKeyWith<>(nodeId, journeyState.getJourneyClock()));
            if (found != null) {
                return HeuristicsReasons.AlreadySeenRouteStation(howIGotHere);
            }
        }

        if (labels.contains(GraphLabel.SERVICE)) {
            final HeuristicsReason found = servicePrevious.getIfPresent(nodeId);
            if (found != null) {
                return found;
            }
        }

        return HeuristicsReasons.CacheMiss(howIGotHere);
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        final List<Pair<String, CacheStats>> results = new ArrayList<>();
        results.add(Pair.of("timeNodePrevious", timeNodePrevious.stats()));
        results.add(Pair.of("hourNodePrevious", hourNodePrevious.stats()));
        results.add(Pair.of("routeStationPrevious", routeStationPrevious.stats()));
        results.add(Pair.of("servicePrevious", servicePrevious.stats()));

        return results;
    }

    public void reportStats() {
        if (cachingDisabled) {
            logger.info("Caching is disabled");
        } else {
            stats().forEach(pair -> logger.info("Cache stats for " + pair.getLeft() + " " + pair.getRight().toString()));
        }
    }

    private static class NodeIdKeyWith<T> {

        private final GraphNodeId nodeId;
        private final T other;
        private final int hashCode;

        public NodeIdKeyWith(GraphNodeId nodeId, T other) {
            this.nodeId = nodeId;
            this.other = other;
            hashCode = Objects.hash(nodeId, other);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeIdKeyWith<?> key = (NodeIdKeyWith<?>) o;
            return Objects.equals(nodeId, key.nodeId) && Objects.equals(other, key.other);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

}
