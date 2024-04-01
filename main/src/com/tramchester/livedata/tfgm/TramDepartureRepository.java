package com.tramchester.livedata.tfgm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.repository.LiveDataObserver;
import com.tramchester.livedata.repository.UpcomingDeparturesSource;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.metrics.HasMetrics;
import com.tramchester.metrics.RegistersMetrics;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class TramDepartureRepository implements UpcomingDeparturesSource, LiveDataObserver, ReportsCacheStats, HasMetrics {
    private static final Logger logger = LoggerFactory.getLogger(TramDepartureRepository.class);

    // TODO Correct limit here?
    private static final Duration TIME_LIMIT_MINS = DeparturesRepository.TRAM_WINDOW; // only enrich if data is within this many minutes
    private static final long STATION_INFO_CACHE_SIZE = 250; // currently 202, see healthcheck for current numbers

    // platformId -> StationDepartureInfo
    private final Cache<IdFor<Platform>, PlatformDueTrams> dueTramsCache;
    private final ProvidesNow providesNow;
    private final LiveDataMarshaller updater;

    private LocalDateTime lastRefresh;

    @Inject
    public TramDepartureRepository(LiveDataMarshaller updater, ProvidesNow providesNow, CacheMetrics registory) {
        this.updater = updater;
        this.providesNow = providesNow;

        dueTramsCache = Caffeine.newBuilder().maximumSize(STATION_INFO_CACHE_SIZE).
                expireAfterWrite(TIME_LIMIT_MINS.getSeconds(), TimeUnit.SECONDS).recordStats().build();

        registory.register(this);
    }

    @PostConstruct
    public void start() {
        updater.addSubscriber(this);
    }

    @PreDestroy
    public void dispose() {
        dueTramsCache.invalidateAll();
    }

    @Override
    public boolean seenUpdate(final List<TramStationDepartureInfo> update) {
        final int count = updateRepository(update);
        return count>0;
    }

    private int updateRepository(final List<TramStationDepartureInfo> departureInfos) {
        final int consumed = consumeDepartInfo(departureInfos);
        dueTramsCache.cleanUp();
        lastRefresh = providesNow.getDateTime();
        logger.info("Update cache. Up to date entries size: " + this.upToDateEntries());
        return consumed;
    }

    private int consumeDepartInfo(final List<TramStationDepartureInfo> departureInfos) {
        IdSet<Platform> platformsSeen = new IdSet<>();

        // TODO See platforms in live data not present in the timetable data
        for (final TramStationDepartureInfo departureInfo : departureInfos) {
            if (departureInfo.hasStationPlatform()) {
                updateCacheFor(departureInfo, platformsSeen);
            } else {
                logger.warn("No platform, skipping " + departureInfo);
            }
        }

        return platformsSeen.size();
    }

    private void updateCacheFor(final TramStationDepartureInfo newDepartureInfo, final IdSet<Platform> platformsSeen) {

        final Platform platform = newDepartureInfo.getStationPlatform();
        final IdFor<Platform> platformId = platform.getId();
        if (!platformsSeen.contains(platformId)) {
            platformsSeen.add(platformId);
            dueTramsCache.put(platformId, new PlatformDueTrams(newDepartureInfo));
            return;
        }

        // many platforms have more than one display, no need to warn about it
        final PlatformDueTrams existingEntry = dueTramsCache.getIfPresent(platformId);
        if (existingEntry!=null) {
            newDepartureInfo.getDueTrams().forEach(dueTram -> {
                if (!existingEntry.hasDueTram(dueTram)) {
                    logger.info(format("Additional due tram '%s' seen for platform id '%s'", dueTram, platformId));
                    existingEntry.addDueTram(dueTram);
                }
            });
        }
    }

    @Override
    public List<UpcomingDeparture> forStation(final Station station) {
        final Set<UpcomingDeparture> allTrams = station.getPlatforms().stream().
                flatMap(platform -> dueTramsForPlatform(platform.getId()).stream()).
                collect(Collectors.toSet());

        if (allTrams.isEmpty()) {
            logger.debug("No trams found for " + station.getId());
            return Collections.emptyList();
        }

        List<UpcomingDeparture> dueTrams = allTrams.stream().
                filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                collect(Collectors.toList());

        if (dueTrams.isEmpty()) {
            logger.info("No DUE trams found for " + station.getId());
        }

        return dueTrams;
    }

    private List<UpcomingDeparture> dueTramsForPlatform(final IdFor<Platform> platform) {
        if (lastRefresh==null) {
            logger.warn("No refresh has happened");
            return Collections.emptyList();
        }
        final Optional<PlatformDueTrams> maybe = departuresFor(platform);
        if (maybe.isEmpty()) {
            logger.info("No due trams found for platform: " + platform);
            return Collections.emptyList();
        }

        final PlatformDueTrams departureInfo = maybe.get();
        return departureInfo.getDueTrams();
    }

    private boolean withinTime(final TramTime queryTime, final LocalTime updateTime) {
        final TramTime limitBefore = TramTime.ofHourMins(updateTime.minus(TIME_LIMIT_MINS));
        final TramTime limitAfter = TramTime.ofHourMins(updateTime.plus(TIME_LIMIT_MINS));
        return queryTime.between(limitBefore, limitAfter);
    }

    private Optional<PlatformDueTrams> departuresFor(IdFor<Platform> platformId) {
        PlatformDueTrams ifPresent = dueTramsCache.getIfPresent(platformId);

        if (ifPresent==null) {
            logger.warn("Could not find departure info for " + platformId);
            return Optional.empty();
        }
        return Optional.of(ifPresent);
    }


    @Override
    public List<Pair<String, CacheStats>> stats() {
        return Collections.singletonList(Pair.of("PlatformMessageRepository:messageCache", dueTramsCache.stats()));
    }

    // for healthcheck
    public int upToDateEntries() {
        dueTramsCache.cleanUp();
        return (int) dueTramsCache.estimatedSize();
    }

    // for healthcheck
    public int getNumStationsWithData(final LocalDateTime queryDateTime) {
        if (lastRefresh==null) {
            logger.error("Never received any data");
            return 0;
        }
        if (!queryDateTime.toLocalDate().equals(lastRefresh.toLocalDate())) {
            return 0;
        }

        final TramTime queryTime = TramTime.ofHourMins(queryDateTime.toLocalTime());
        return getEntryStream(queryTime).
                map(PlatformDueTrams::getStation).collect(Collectors.toSet()).size();
    }

    public int getNumStationsWithTrams(final LocalDateTime dateTime) {
        if (!dateTime.toLocalDate().equals(lastRefresh.toLocalDate())) {
            return 0;
        }

        final TramTime queryTime = TramTime.ofHourMins(dateTime.toLocalTime());
        return getEntryStream(queryTime).filter(entry -> !entry.getDueTrams().isEmpty())
                .map(PlatformDueTrams::getStation).collect(Collectors.toSet()).size();
    }

    private Stream<PlatformDueTrams> getEntryStream(final TramTime queryTime) {
        return dueTramsCache.asMap().values().stream().
                filter(entry -> withinTime(queryTime, entry.getLastUpdate().toLocalTime()));
    }

    @Override
    public void registerMetrics(RegistersMetrics registersMetrics) {
        registersMetrics.add(this, "liveData", "number", this::upToDateEntries);
        registersMetrics.add(this, "liveData", "stationsWithData", this::getNumStationsWithDataNow);
        registersMetrics.add(this,"liveData", "stationsWithTrams", this::getNumStationsWithTramsNow);
    }

    @Override
    public boolean areMetricsEnabled() {
        // todo should depend on whether live data enabled?
        return true;
    }

    private Integer getNumStationsWithDataNow() {
        return getNumStationsWithData(providesNow.getDateTime());
    }

    private Integer getNumStationsWithTramsNow() {
        return getNumStationsWithTrams(providesNow.getDateTime());
    }



    private static class PlatformDueTrams {
        private final Platform stationPlatform;
        private final List<UpcomingDeparture> dueTrams;
        private final LocalDateTime lastUpdate;
        private final IdFor<Station> stationId;

        private PlatformDueTrams(Platform stationPlatform, List<UpcomingDeparture> dueTrams, LocalDateTime lastUpdate,
                                 IdFor<Station> stationId) {
            this.stationPlatform = stationPlatform;
            this.dueTrams = dueTrams;
            this.lastUpdate = lastUpdate;
            this.stationId = stationId;
        }

        public PlatformDueTrams(TramStationDepartureInfo departureInfo) {
            this(departureInfo.getStationPlatform(), departureInfo.getDueTrams(), departureInfo.getLastUpdate(),
                    departureInfo.getStation().getId());
        }

        public boolean hasDueTram(UpcomingDeparture dueTram) {
            return dueTrams.contains(dueTram);
        }

        public void addDueTram(UpcomingDeparture dueTram) {
            dueTrams.add(dueTram);
        }

        public LocalDateTime getLastUpdate() {
            return lastUpdate;
        }

        public List<UpcomingDeparture> getDueTrams() {
            return dueTrams;
        }

        public IdFor<Station> getStation() {
            return stationId;
        }

        @Override
        public String toString() {
            return "PlatformDueTrams{" +
                    "stationPlatform=" + stationPlatform.getId() +
                    ", stationId=" + stationId +
                    ", dueTrams=" + dueTrams +
                    ", lastUpdate=" + lastUpdate +
                    '}';
        }
    }
}
