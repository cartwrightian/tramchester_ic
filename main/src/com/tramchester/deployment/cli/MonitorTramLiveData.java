package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.repository.StationRepository;
import io.dropwizard.configuration.ConfigurationException;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

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
        //final MatchLiveTramToJourneyDestination matchLiveTramToJourneyDestination = dependencies.get(MatchLiveTramToJourneyDestination.class);
        final CountDownLatch latch = new CountDownLatch(numberToReceive);

        RecordDisplayInfo recordDisplayInfo = new RecordDisplayInfo(logger);

        marshaller.addSubscriber(updates -> {
//            recordDisplayInfo.record(updates);
//            liveTramDataToCallingPoints.map(updates);
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

        RecordDisplayInfo(Logger logger) {
            this.logger = logger;
        }

        public void record(List<TramStationDepartureInfo> updates, StationRepository stationRepos) {
            Set<StationIdPair> allAmbiguousRoutings = updates.stream().
                    flatMap(update -> record(update, stationRepos).stream()).
                    collect(Collectors.toSet());

            if (!allAmbiguousRoutings.isEmpty()) {
                logger.error("Found:" + allAmbiguousRoutings.size() + " Ambiguous between " + allAmbiguousRoutings);

                IdSet<Station> ambiguousStarts = allAmbiguousRoutings.stream().map(StationIdPair::getBeginId).collect(IdSet.idCollector());
                IdSet<Station> ambiguousEnds = allAmbiguousRoutings.stream().map(StationIdPair::getEndId).collect(IdSet.idCollector());

                logger.info("Starts: " +ambiguousStarts.size() + " " + ambiguousStarts);
                logger.info("Ends:" + ambiguousEnds.size() + " " + ambiguousEnds);
            }
        }

        private Set<StationIdPair> record(TramStationDepartureInfo update, StationRepository stationRepos) {
            Station displayStation = update.getStation();

            return update.getDueTrams().stream().
                    map(UpcomingDeparture::getDestinationId).
                    filter(dest -> !dest.equals(displayStation.getId())).
                    filter(dest -> ambiguousRoutingBetween(update, dest, stationRepos)).
                    map(dest -> StationIdPair.of(update.getStation().getId(), dest)).
                    collect(Collectors.toSet());

        }

        /**
         * @param tramStationDepartureInfo the live display information
         * @param destinationId the destination of the due tram
         * @return true if ambiguous
         */
        private boolean ambiguousRoutingBetween(TramStationDepartureInfo tramStationDepartureInfo, IdFor<Station> destinationId, StationRepository stationRepository) {
            Station displayStation = tramStationDepartureInfo.getStation();

            final Set<Route> pickups;
            if (tramStationDepartureInfo.hasStationPlatform()) {
                final Platform platform = tramStationDepartureInfo.getStationPlatform();
                pickups = platform.getPickupRoutes();
            } else {
                pickups = displayStation.getPickupRoutes();
            }


            Station destination = stationRepository.getStationById(destinationId);
            Set<Route> dropoffs = destination.getDropoffRoutes();

            SetUtils.SetView<Route> overlap = SetUtils.union(pickups, dropoffs);

            String suffix = " between " + displayStation.getId() + " and " + destinationId;

            if (overlap.isEmpty()) {
                logger.error("No overlap " + suffix);
                return false;
            }

            if (overlap.size()==1) {
                logger.debug("Only one route (" + HasId.asIds(overlap) + ") " + suffix);
                return false;
            }

            // appears this does not happen when we have more than one route....
            boolean sameCallingPoints = sameCallingPoints(overlap, displayStation, destinationId);
            if (sameCallingPoints) {
                logger.info("Same calling points for routes " + HasId.asIds(overlap) + suffix);
            }
            return !sameCallingPoints;
        }

        private boolean sameCallingPoints(SetUtils.SetView<Route> overlap, Station start, IdFor<Station> end) {
            Set<List<Station>> allCallingPoints = new HashSet<>();
            for(Route route : overlap) {
                route.getTrips().forEach(trip -> {
                    if (trip.callsAt(start.getId()) && trip.callsAt(end)) {
                        List<Station> callingPoints = new ArrayList<>();
                        final StopCalls stopCalls = trip.getStopCalls();
                        final int firstIndex = stopCalls.getStopFor(start.getId()).getGetSequenceNumber();
                        final int lastIndex = stopCalls.getStopFor(end).getGetSequenceNumber();
                        for (int i = firstIndex; i <= lastIndex; i++) {
                            callingPoints.add(stopCalls.getStopBySequenceNumber(i).getStation());
                        }
                        allCallingPoints.add(callingPoints);
                    }
                });
            }

            boolean sameCallingPoints = allCallingPoints.size() == 1;
            if (sameCallingPoints) {
                // unambiguous
                // seems this  not happen, if multiple routes we always end up with an ambiguous set of calling points
                logger.info("Unambiguous calling points between " + start.getId() + " and " + end
                        + " for routes " + HasId.asIds(overlap));
            } else {
                logger.error("Ambiguous calling points between " + start.getId() + " and " + end
                        + " for routes " + HasId.asIds(overlap));
            }

            return sameCallingPoints;
        }

        public void logResults() {

        }
    }

}
