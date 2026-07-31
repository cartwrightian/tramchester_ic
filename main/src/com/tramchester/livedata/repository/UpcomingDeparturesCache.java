package com.tramchester.livedata.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UpcomingDeparturesCache  {
    private static final Logger logger = LoggerFactory.getLogger(UpcomingDeparturesCache.class);

    private final DeparturesCache cache;
    private final CacheMetrics cacheMetrics;

    public UpcomingDeparturesCache(long size, TramDuration duration, CacheMetrics cacheMetrics) {
        this.cacheMetrics = cacheMetrics;
        cache = new DeparturesCache(size, duration);
    }

    public List<UpcomingDeparture> getOrUpdate(final Station station, final CacheUpdateStrategy cacheUpdateStrategy) {
        logger.info("Get for " + station.getId());
        return cache.getOrUpdate(station, cacheUpdateStrategy);
    }


    public void start() {
        logger.info("starting " + this);
        cacheMetrics.register(cache);
        logger.info("started " + this);
    }

    private static class DeparturesCache implements ReportsCacheStats {
        // NOTE: have to split caches as only calling points are only present for the Detailed query
        private final Cache<Station, List<UpcomingDeparture>> cacheGeneral;
        private final Cache<Station, List<UpcomingDeparture>> cacheDetails;

        private DeparturesCache(final long size, final TramDuration duration) {
            cacheGeneral = createCache(size, duration);
            cacheDetails = createCache(size, duration);
        }

        private static @NonNull Cache<Station, List<UpcomingDeparture>> createCache(long size, TramDuration duration) {
            return Caffeine.newBuilder().maximumSize(size).
                    expireAfterWrite(duration.toDuration()).
                    initialCapacity((int) size).
                    recordStats().build();
        }

        public List<UpcomingDeparture> getOrUpdate(final Station station, final CacheUpdateStrategy cacheUpdateStrategy) {
            if (cacheUpdateStrategy.isDetailed()) {
                logger.info("detailed departures for " + station.getId());
                return cacheDetails.get(station, key -> cacheUpdateStrategy.fetchFor(station));
            } else {
                logger.info("general departures for " + station.getId());
                return cacheGeneral.get(station, key -> cacheUpdateStrategy.fetchFor(station));
            }
        }

        @Override
        public List<Pair<String, CacheStats>> stats() {
            return List.of(
                    Pair.of("UpcomingDeparturesCache:cacheGeneral", cacheGeneral.stats()),
                    Pair.of("UpcomingDeparturesCache:cacheDetails", cacheDetails.stats())
            );
        }
    }

    public interface CacheUpdateStrategy {
        List<UpcomingDeparture> fetchFor(Station station);
        boolean isDetailed(); // i.e. we are asking for calling points
    }
}
