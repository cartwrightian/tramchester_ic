package com.tramchester.livedata.tfgm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.LiveDataObserver;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.metrics.HasMetrics;
import com.tramchester.metrics.RegistersMetrics;
import com.tramchester.repository.ReportsCacheStats;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@LazySingleton
public class PlatformMessageRepository implements PlatformMessageSource, ReportsCacheStats, HasMetrics, LiveDataObserver {
    private static final Logger logger = LoggerFactory.getLogger(PlatformMessageRepository.class);

    private static final int TIME_LIMIT = 20; // only enrich if data is within this many minutes
    private static final long STATION_INFO_CACHE_SIZE = 250; // currently 202, see healthcheck for current numbers
    public static final int EMPTY_MESSAGE_WARN_THRESHOLD = 10;

    private final Cache<IdFor<Platform>, PlatformMessage> messageCache;
    private final LiveDataMarshaller updater;
    private final ProvidesNow providesNow;
    private final CacheMetrics cacheMetrics;
    private final TramchesterConfig config;
    private TramDate lastRefresh;

    @Inject
    public PlatformMessageRepository(LiveDataMarshaller updater, ProvidesNow providesNow, CacheMetrics cacheMetrics, TramchesterConfig config) {
        this.updater = updater;
        this.providesNow = providesNow;
        this.cacheMetrics = cacheMetrics;
        this.config = config;
        messageCache = Caffeine.newBuilder().maximumSize(STATION_INFO_CACHE_SIZE).
                expireAfterWrite(TIME_LIMIT, TimeUnit.MINUTES).recordStats().build();

    }

    @PostConstruct
    public void start() {
        if (areMetricsEnabled()) {
            cacheMetrics.register(this);
        } else {
            logger.warn("Disabled, live data is not configured");
        }
        updater.addSubscriber(this);
    }

    @PreDestroy
    public void dispose() {
        messageCache.invalidateAll();
    }

    @Override
    public boolean areMetricsEnabled() {
        return isEnabled();
    }

    @Override
    public boolean isEnabled() {
        return config.liveTfgmTramDataEnabled();
    }


    @Override
    public boolean seenUpdate(List<TramStationDepartureInfo> received) {
        int entries = updateCache(received);
        return entries>0;
    }

    public int updateCache(List<TramStationDepartureInfo> departureInfos) {
        if (!isEnabled()) {
            logger.error("Unexpected call of updateCache since live data is disabled");
        }
        logger.debug("Updating cache");
        consumeDepartInfo(departureInfos);
        messageCache.cleanUp();
        lastRefresh = providesNow.getTramDate();
        int entries = numberOfEntries();
        logger.info("Cache now has " + entries + " entries");

        return entries;
    }

    private void consumeDepartInfo(final List<TramStationDepartureInfo> departureInfos) {
        IdSet<Platform> platformsSeen = new IdSet<>();
        int emptyMessages = 0;
        int noTrams = 0;

        for (final TramStationDepartureInfo departureInfo : departureInfos) {
            if (departureInfo.hasStationPlatform()) {
                if (!updateCacheFor(departureInfo, platformsSeen)) {
                    emptyMessages = emptyMessages + 1;
                }
            } else {
                logger.warn("No platform, skipping " + departureInfo);
            }
            if (!departureInfo.hasDueTrams()) {
                noTrams = noTrams + 1;
            }
        }

        if (emptyMessages > EMPTY_MESSAGE_WARN_THRESHOLD) {
            logger.warn("Received "+emptyMessages+" empty messages");
        } else if (emptyMessages>0) {
            logger.info("Received "+emptyMessages+" empty messages");
        }
    }

    private boolean updateCacheFor(final TramStationDepartureInfo departureInfo, final IdSet<Platform> platformsSeenForUpdate) {

        final Platform stationPlatform = departureInfo.getStationPlatform();
        final IdFor<Platform> platformId = stationPlatform.getId();
        if (platformsSeenForUpdate.contains(platformId)) {
            if (!departureInfo.getMessage().isEmpty()) {
                final PlatformMessage current = messageCache.getIfPresent(platformId);
                if (current!=null) {
                    // todo have multiple messages per platform? This means displays at that platform are showing
                    // different information. This does happen occasionally.....
                    final String currentMessage = current.getMessage();
                    if (!departureInfo.getMessage().equals(currentMessage)) {
                        logger.warn("Multiple messages for " + platformId + " displayId: " + departureInfo.getDisplayId() +
                                " Received: '" + departureInfo.getMessage() + "' was '" + currentMessage + "' displayId: "
                                + current.getDisplayId());
                    }
                } else {
                    logger.error("Could not find message in cache for " + platformId);
                }

            } else {
                logger.info("Multiple (but empty) message for " + platformId + " displayId: " + departureInfo.getDisplayId());
            }
            return false;
        }

        if (departureInfo.getMessage().isEmpty()) {
            logger.debug("Skipping empty message for " +platformId+ " displayId: " + departureInfo.getDisplayId());
            return false;
        }

        platformsSeenForUpdate.add(platformId);
        messageCache.put(platformId, new PlatformMessage(departureInfo));
        return true;
    }

    @Override
    public List<PlatformMessage> messagesFor(Station station, TramDate when, TramTime queryTime) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }
        if (!station.getTransportModes().contains(TransportMode.Tram)) {
            logger.info("Not a tram station, not checking for messages for " + station.getId());
            return Collections.emptyList();
        }

        // TODO this uses the timetable station/platform association, but seems live data includes extra platforms
        // not provided in stops.txt feed
        logger.info("Get messages for " + HasId.asId(station));
        List<PlatformMessage> results = new ArrayList<>();
        station.getPlatforms().forEach(platform -> messagesFor(platform.getId(), when, queryTime).ifPresent(results::add));
        if (results.isEmpty()) {
            logger.warn("No platform messages found for " + HasId.asId(station));
        }
        return results;
    }

    @Override
    public Optional<PlatformMessage> messagesFor(IdFor<Platform> platformId, TramDate queryDate, TramTime queryTime) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        if (lastRefresh==null) {
            logger.warn("No refresh has happened");
            return Optional.empty();
        }
        if (!queryDate.equals(lastRefresh)) {
            logger.warn("No data for date, not querying for departure info " + queryDate);
            return Optional.empty();
        }
        Optional<PlatformMessage> maybe = messagesFor(platformId);
        if (maybe.isEmpty()) {
            logger.info("No message found for platform: " + platformId);
            return Optional.empty();
        }
        PlatformMessage departureInfo = maybe.get();

        LocalDateTime infoLastUpdate = departureInfo.getLastUpdate();
        if (!withinTime(queryTime, infoLastUpdate.toLocalTime())) {
            logger.info("last update of departure info (" + infoLastUpdate +") not within query time " + queryTime);
            return Optional.empty();
        }
        return Optional.of(departureInfo);
    }

    private boolean withinTime(TramTime queryTime, LocalTime updateTime) {
        TramTime limitBefore = TramTime.ofHourMins(updateTime.minusMinutes(TIME_LIMIT));
        TramTime limitAfter = TramTime.ofHourMins(updateTime.plusMinutes(TIME_LIMIT));
        return queryTime.between(limitBefore, limitAfter);
    }

    public int numberOfEntries() {
        messageCache.cleanUp();
        return (int) messageCache.estimatedSize();
    }

    private Optional<PlatformMessage> messagesFor(IdFor<Platform> platformId) {
        @Nullable PlatformMessage ifPresent = messageCache.getIfPresent(platformId);

        if (ifPresent==null) {
            return Optional.empty();
        }
        return Optional.of(ifPresent);
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return Collections.singletonList(
                Pair.of("PlatformMessageRepository:messageCache", messageCache.stats()));
    }

    // for healthcheck
    public int numberStationsWithMessages(LocalDateTime queryDateTime) {
        if (!areMetricsEnabled()) {
            return 0;
        }

        if (!lastRefresh.toLocalDate().equals(queryDateTime.toLocalDate())) {
            return 0;
        }

        Set<Station> haveMessages = getStationsWithMessages(queryDateTime.toLocalTime());
        logger.debug("Stations with messages " + haveMessages);
        return haveMessages.size();
    }

    // for metrics
    private Integer numberStationsWithMessagesNow() {
        return numberStationsWithMessages(providesNow.getDateTime());
    }

    @NotNull
    public Set<Station> getStationsWithMessages(LocalTime time) {
        TramTime queryTime = TramTime.ofHourMins(time);
        return messageCache.asMap().values().stream().
                filter(entry -> withinTime(queryTime, entry.getLastUpdate().toLocalTime())).
                map(PlatformMessage::getStation).collect(Collectors.toSet());
    }

    @Override
    public void registerMetrics(RegistersMetrics registersMetrics) {
        registersMetrics.add(this, "liveData", "messages", this::numberOfEntries);
        registersMetrics.add(this, "liveData", "stationsWithMessages", this::numberStationsWithMessagesNow);
    }

}
