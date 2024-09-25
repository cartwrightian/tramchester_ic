package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.livedata.tfgm.OverheadDisplayLines;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/***
 * Tool to assist with understanding how the line data Line, Direction, DisplayID and destinations
 * relate the Stations and Routes from the timetable data
 */
public class MonitorTramLiveData extends BaseCLI {

    private final int numberToReceive;

    public MonitorTramLiveData(int numberToReceive) {
        super();
        this.numberToReceive = numberToReceive;
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(MonitorTramLiveData.class);

        if (args.length != 2) {
            throw new RuntimeException("Expected 2 arguments: <config filename> <num messages>");
        }
        final Path configFile = Paths.get(args[0]).toAbsolutePath();
        final int numberToReceive = Integer.parseInt(args[1]);

        logger.info("Config from " + configFile);
        logger.info("Number of messages to receive " + numberToReceive);

        final MonitorTramLiveData cli = new MonitorTramLiveData(numberToReceive);

        try {
            if (!cli.run(configFile, logger, "MonitorTramLiveData")) {
                logger.error("Check logs, not successful");
                System.exit(-1);
            }

        } catch (ConfigurationException | IOException e) {
            logger.error("Exception", e);
            System.exit(-1);
        }
        logger.info("Success");
    }

    @Override
    public boolean run(Logger logger, GuiceContainerDependencies dependencies, TramchesterConfig config) {
        if (!config.liveTfgmTramDataEnabled()) {
            logger.error("Live data needs to be enabled in config");
            return false;
        }

        final TfgmTramLiveDataConfig liveConfig = config.getLiveDataConfig();
        final Long refreshSeconds = liveConfig.getRefreshPeriodSeconds();

        final LiveDataMarshaller marshaller = dependencies.get(LiveDataMarshaller.class);
        final LiveDataFetcher fetcher = dependencies.get(LiveDataFetcher.class);
        final CountDownLatch latch = new CountDownLatch(numberToReceive);

        RecordDisplayInfo recordDisplayInfo = new RecordDisplayInfo(logger);

        marshaller.addSubscriber(updates -> {
            recordDisplayInfo.record(updates);
            latch.countDown();
            logger.info("Received " + updates.size() + " updates");
            return true;
        });

        fetcher.subscribe(text -> logger.info("Received " + text.length() + " bytes of data"));

        logger.info("Refresh interval is " + refreshSeconds + " seconds");

        final TimerTask fetchDataTask = new TimerTask() {
            @Override
            public void run() {
                fetcher.fetch();
            }
        };

        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(fetchDataTask, 0, refreshSeconds * 1000);

        try {
            latch.await();
            recordDisplayInfo.logResults();
            logger.info("finished normally");
        } catch (InterruptedException e) {
            logger.error("Failed to wait for latch", e);
        }

        logger.info("Finishing");
        timer.cancel();
        return true;
    }

    private static class RecordDisplayInfo {
        private final Logger logger;
        private final Map<String, RecordedDisplayInfo> displayInfos;
        private final Map<LineAndDirection, IdSet<Station>> destinations;

        RecordDisplayInfo(Logger logger) {
            this.logger = logger;
            displayInfos = new HashMap<>();
            destinations = new HashMap<>();
        }

        public void record(List<TramStationDepartureInfo> updates) {
            updates.forEach(this::record);
        }

        private void record(TramStationDepartureInfo update) {
            String displayId = update.getDisplayId();
            LineAndDirection lineAndDirection = new LineAndDirection(update.getLine(), update.getDirection());
            RecordedDisplayInfo recordedDisplayInfo = new RecordedDisplayInfo(update.getStation().getId(), lineAndDirection);
            if (!displayInfos.containsKey(displayId)) {
                displayInfos.put(displayId, recordedDisplayInfo);
            } else {
                RecordedDisplayInfo existing = displayInfos.get(displayId);
                if (!recordedDisplayInfo.equals(existing)) {
                    logger.warn("Mismatch for display " + displayId + " between " + existing + " and " + recordedDisplayInfo);
                }
            }

            if (!destinations.containsKey(lineAndDirection)) {
                destinations.put(lineAndDirection, new IdSet<>());
            }
            IdSet<Station> tramDestinations = update.getDueTrams().stream().map(UpcomingDeparture::getDestination).collect(IdSet.collector());
            destinations.get(lineAndDirection).addAll(tramDestinations);
        }

        public void logResults() {
            logger.info("Results");
            displayInfos.entrySet().forEach(entry -> {
                logger.info(entry.getKey() + " -> " + entry.getValue());
            });
            destinations.entrySet().forEach(entry -> {
                logger.info(entry.getKey() + " -> " + entry.getValue());
            });
        }
    }

    private record LineAndDirection(OverheadDisplayLines line, LineDirection direction) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LineAndDirection that)) return false;
            return direction == that.direction && line == that.line;
        }

        @Override
        public String toString() {
            return "{" +
                    "line=" + line +
                    ", direction=" + direction +
                    '}';
        }
    }

    private record RecordedDisplayInfo(IdFor<Station> id, LineAndDirection lineAndDirection) {

        @Override
        public String toString() {
            return "RecordedDisplayInfo{" +
                    "id=" + id +
                    ", lineAndDirection=" + lineAndDirection +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RecordedDisplayInfo that)) return false;
            return Objects.equals(id, that.id) && lineAndDirection == that.lineAndDirection;
        }

    }

}
