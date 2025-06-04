package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO NOW ONLY USED FOR TEST SUPPORT, MOVE

@LazySingleton
public class TripEndsRepository {
    private static final Logger logger = LoggerFactory.getLogger(TripEndsRepository.class);

    private final TripRepository tripRepository;
    private final Map<TransportMode, IdSet<Station>> firstAndLastStops;
    private final Set<TransportMode> enabledModes;
    private final StationRepository stationRepository;

    @Inject
    public TripEndsRepository(TripRepository tripRepository, TramchesterConfig config, StationRepository stationRepository) {
        this.tripRepository = tripRepository;
        this.stationRepository = stationRepository;

        firstAndLastStops = new EnumMap<>(TransportMode.class);
        enabledModes = config.getTransportModes();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        for(TransportMode mode : enabledModes) {

            final IdSet<Station> firstStops = getFirstStopsFor(mode);
            final IdSet<Station> lastStops = getLastStopsFor(mode);

            final IdSet<Station> allStops = firstStops.addAll(lastStops);

            firstAndLastStops.put(mode, allStops);

            logger.info("Found " + allStops.size() + " ends of routes for " + mode);
        }

        logger.info("started");
    }

    private IdSet<Station> getFirstStopsFor(final TransportMode mode) {
        Set<StopCall> stopCalls = tripRepository.getTrips().stream().
                filter(trip -> !trip.isFiltered()).
                filter(trip -> trip.getTransportMode().equals(mode)).
                map(trip -> trip.getStopCalls().getFirstStop()).
                collect(Collectors.toSet());
        return validateStations(stopCalls);
    }

    private IdSet<Station> getLastStopsFor(final TransportMode mode) {
        Set<StopCall> stopCalls = tripRepository.getTrips().stream().
                filter(trip -> !trip.isFiltered()).
                filter(trip -> trip.getTransportMode().equals(mode)).
                map(trip -> trip.getStopCalls().getLastStop()).
                collect(Collectors.toSet());
        return validateStations(stopCalls);
    }

    private IdSet<Station> validateStations(Set<StopCall> stopCalls) {
        Set<StopCall> missingStations = stopCalls.stream().
                filter(stopCall -> !stationRepository.hasStationId(stopCall.getStationId())).
                collect(Collectors.toSet());
        if (!missingStations.isEmpty()) {
            throw new RuntimeException("Missing station ids for " + missingStations);
        }

        Set<StopCall> inactiveStations = stopCalls.stream().
                filter(stopCall -> !stationRepository.getStationById(stopCall.getStationId()).isActive()).
                collect(Collectors.toSet());
        if (!inactiveStations.isEmpty()) {
            throw new RuntimeException("Inaction stations for " + inactiveStations);
        }

        return stopCalls.stream().map(StopCall::getStationId).collect(IdSet.idCollector());
    }

    @PreDestroy
    public void stop() {
        logger.info("Stop");
        firstAndLastStops.values().forEach(IdSet::clear);
        firstAndLastStops.clear();
        logger.info("stopped");
    }

    public IdSet<Station> getStations(final EnumSet<TransportMode> modes) {
        return modes.stream().
                flatMap(mode -> firstAndLastStops.get(mode).stream()).
                collect(IdSet.idCollector());
    }

    public IdSet<Station> getStations(TransportMode transportMode) {
        return getStations(EnumSet.of(transportMode));
    }
}
