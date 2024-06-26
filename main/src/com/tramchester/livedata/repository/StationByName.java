package com.tramchester.livedata.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@LazySingleton
public class StationByName {
    private static final Logger logger = LoggerFactory.getLogger(StationByName.class);

    private final StationRepository stationRepository;

    private final Map<String, Station> tramStationsByName = new HashMap<>();  // tram station name -> station

    @Inject
    public StationByName(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        stationRepository.getActiveStationStream().
                filter(TransportMode::isTram).
                forEach(station -> tramStationsByName.put(station.getName().toLowerCase(), station));
        logger.info("Added " + tramStationsByName.size() + " tram stations");
        logger.info("started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("stopping");
        tramStationsByName.clear();
        logger.info("stopped");
    }

    // used for live data association
    public Optional<Station> getTramStationByName(String name) {
        String lowerCase = name.toLowerCase();
        if (tramStationsByName.containsKey(lowerCase)) {
            return Optional.of(tramStationsByName.get(lowerCase));
        } else {
            return Optional.empty();
        }
    }

}
