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
        fetcher.subscribe(this);
        logger.info("started");
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

        int received = receivedInfos.size();
        logger.info(format("Received %s updates", received));

        List<TramStationDepartureInfo> fresh = filterForFreshness(receivedInfos);
        int freshCount = fresh.size();
        String msg = freshCount + " of received " + received + " are fresh";
        if (freshCount > 0) {
            logger.info(msg);
        } else {
            logger.error(msg);
        }

        if (!fresh.isEmpty()) {
            invokeObservers(fresh);
        }

        fresh.clear();
        receivedInfos.clear();
    }

    @NotNull
    private List<TramStationDepartureInfo> filterForFreshness(List<TramStationDepartureInfo> receivedInfos) {
        TramTime now = providesNow.getNowHourMins();
        LocalDate date = providesNow.getDate();
        int stale = 0;

        List<TramStationDepartureInfo> fresh = new ArrayList<>();
        for (TramStationDepartureInfo departureInfo : receivedInfos) {
            if (isTimely(departureInfo, date, now)) {
                fresh.add(departureInfo);
            } else {
                stale = stale + 1;
                logger.warn("Received stale departure info " + departureInfo);
            }
        }
        if (stale >0) {
            logger.warn("Received " + stale + " stale messages out of " + receivedInfos.size());
        }
        if (fresh.isEmpty()) {
            logger.warn("Got zero fresh messages");
        }
        return fresh;
    }

    private boolean isTimely(TramStationDepartureInfo newDepartureInfo, LocalDate date, TramTime now) {
        LocalDate updateDate = newDepartureInfo.getLastUpdate().toLocalDate();
        if (!updateDate.equals(date)) {
            logger.info("Received invalid update, date was " + updateDate);
            return false;
        }
        TramTime updateTime = TramTime.ofHourMins(newDepartureInfo.getLastUpdate().toLocalTime());

        if (Durations.greaterThan(TramTime.difference(now, updateTime), TIME_LIMIT)) {
            logger.info(format("Received out of date update. Local Now: %s Update: %s ", providesNow.getNowHourMins(), updateTime));
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


}
