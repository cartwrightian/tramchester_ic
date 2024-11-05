package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.repository.LiveDataObserver;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class LiveDataMarshaller implements LiveDataFetcher.ReceivesRawData {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataMarshaller.class);

    private static final Duration TIME_LIMIT = Duration.ofMinutes(20); // only enrich if data is within this many minutes

    private final List<LiveDataObserver> observers;

    private final LiveDataFetcher fetcher;
    private final LiveDataParser parser;
    private final ProvidesNow providesNow;

    @Inject
    public LiveDataMarshaller(LiveDataFetcher fetcher, LiveDataParser parser, ProvidesNow providesNow) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.providesNow = providesNow;

        observers = new LinkedList<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        if (fetcher.isEnabled()) {
            fetcher.subscribe(this);
            logger.info("started");
        } else {
            String message = "LiveDataFetcher is disabled, not subscribing. Is live data disabled?";
            logger.error(message);
            // todo throw here?
            //throw new RuntimeException(message);
        }
    }

    @PreDestroy
    public void dispose() {
        fetcher.stop();
        observers.clear();
    }

    @Override
    public void rawData(String text) {
        processRawData(text);
    }

    private void processRawData(final String payload) {
        final List<TramStationDepartureInfo> receivedInfos;
        if (payload.isEmpty()) {
            logger.warn("Empty payload");
            receivedInfos = Collections.emptyList();
        } else {
            receivedInfos = parser.parse(payload);
        }

        final int received = receivedInfos.size();
        logger.debug(format("Received %s updates", received));

        final FreshAndStale freshAndStale = filterForFreshness(receivedInfos);

        freshAndStale.logResults();

        if (freshAndStale.hasAnyFresh()) {
            invokeObservers(freshAndStale.getFresh());
        }

        freshAndStale.clear();
        receivedInfos.clear();
    }

    @NotNull
    private FreshAndStale filterForFreshness(final List<TramStationDepartureInfo> receivedInfos) {
        final TramTime now = providesNow.getNowHourMins();
        final LocalDate date = providesNow.getDate();

        FreshAndStale freshAndStale = new FreshAndStale();

        for (TramStationDepartureInfo departureInfo : receivedInfos) {
            if (isTimely(departureInfo, date, now)) {
                freshAndStale.addFresh(departureInfo);
            } else {
                freshAndStale.addStale(departureInfo);
            }
        }
        return freshAndStale;
    }

    private boolean isTimely(final TramStationDepartureInfo newDepartureInfo, final LocalDate date, final TramTime now) {
        final LocalDate updateDate = newDepartureInfo.getLastUpdate().toLocalDate();
        if (!updateDate.equals(date)) {
            logger.warn("Received invalid update, date was " + updateDate);
            return false;
        }

        final TramTime updateTime = TramTime.ofHourMins(newDepartureInfo.getLastUpdate().toLocalTime());
        if (Durations.greaterThan(TramTime.difference(now, updateTime), TIME_LIMIT)) {
            //logger.info(format("Received out of date update. Local Now: %s Update: %s ", providesNow.getNowHourMins(), updateTime));
            return false;
        }

        return true;
    }

    private void invokeObservers(List<TramStationDepartureInfo> receivedInfos) {
        try {
            observers.forEach(observer -> observer.seenUpdate(receivedInfos));
        }
        catch (RuntimeException runtimeException) {
            logger.error("Error invoking observer", runtimeException);
        }
    }

    public void addSubscriber(LiveDataObserver observer) {
        logger.info("Add subscriber " + observer.getClass().getSimpleName());
        observers.add(observer);
    }

    private static class FreshAndStale {
        public static final int REPORTING_THRESHOLD = 10;
        private final List<TramStationDepartureInfo> fresh;
        private final List<TramStationDepartureInfo> stale;

        private FreshAndStale() {
            fresh = new ArrayList<>();
            stale = new ArrayList<>();
        }

        public void addFresh(TramStationDepartureInfo departureInfo) {
            fresh.add(departureInfo);
        }

        public void addStale(TramStationDepartureInfo departureInfo) {
            stale.add(departureInfo);
        }

        public void clear() {
            fresh.clear();
            stale.clear();
        }

        public boolean hasAnyFresh() {
            return !fresh.isEmpty();
        }

        public List<TramStationDepartureInfo> getFresh() {
            return fresh;
        }

        public void logResults() {
            if (fresh.isEmpty()) {
                logger.error("No fresh results received, all " + stale.size() + " were stale");
                return;
            }
            if (stale.isEmpty()) {
                return;
            }
            if (stale.size()< REPORTING_THRESHOLD) {
                logStaleResults();
            } else {
                logger.error("More than " + REPORTING_THRESHOLD + " results were stale");
            }
        }

        private void logStaleResults() {
            List<String> ids = stale.stream().
                    map(departInfo -> "station:" + departInfo.getStation().getId() + " displayId:"+departInfo.getDisplayId())
                    .toList();
            logger.warn("The following stations have stale departure information " + ids);
        }
    }


}
