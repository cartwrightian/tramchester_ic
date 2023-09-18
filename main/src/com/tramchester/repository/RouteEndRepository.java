package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// TODO NOW ONLY USED FOR TEST SUPPORT, MOVE

@LazySingleton
public class RouteEndRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteEndRepository.class);

    private final TripRepository tripRepository;
    private final Map<TransportMode,IdSet<Station>> beginOrEndOfRoutes;
    private final Set<TransportMode> enabledModes;

    @Inject
    public RouteEndRepository(TripRepository tripRepository, TramchesterConfig config) {
        this.tripRepository = tripRepository;
        beginOrEndOfRoutes = new HashMap<>();
        enabledModes = config.getTransportModes();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        for(TransportMode mode : enabledModes) {

            IdSet<Station> begins = getBegins(mode);
            IdSet<Station> ends = getEnds(mode);

            IdSet<Station> beginsAndEnds = begins.addAll(ends);

            beginOrEndOfRoutes.put(mode, beginsAndEnds);
            logger.info("Found " + beginsAndEnds.size() + " ends of routes for " + mode);
        }

        logger.info("started");
    }

    private IdSet<Station> getBegins(TransportMode mode) {
        return tripRepository.getTrips().stream().
                filter(trip -> !trip.isFiltered()).
                filter(trip -> trip.getTransportMode().equals(mode)).
                map(trip -> trip.getStopCalls().getFirstStop()).
                map(StopCall::getStation).
                collect(IdSet.collector());
    }

    private IdSet<Station> getEnds(TransportMode mode) {
        return tripRepository.getTrips().stream().
                filter(trip -> !trip.isFiltered()).
                filter(trip -> trip.getTransportMode().equals(mode)).
                map(trip -> trip.getStopCalls().getFirstStop()).
                map(StopCall::getStation).
                collect(IdSet.collector());
    }

    @PreDestroy
    public void stop() {
        logger.info("Stop");
        beginOrEndOfRoutes.values().forEach(IdSet::clear);
        beginOrEndOfRoutes.clear();
        logger.info("stopped");
    }

    public IdSet<Station> getStations(TransportMode mode) {
        return beginOrEndOfRoutes.get(mode);
    }

}
