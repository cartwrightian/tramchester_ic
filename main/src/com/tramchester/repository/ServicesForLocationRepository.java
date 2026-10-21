package com.tramchester.repository;

import com.google.inject.Inject;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.LocationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@LazySingleton
public class ServicesForLocationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ServicesForLocationRepository.class);

    private final StationRepository stationRepository;
    private final TripRepository tripRepository;
    private final Map<LocationId<?>, Set<Service>> servicesForLocations;

    @Inject
    public ServicesForLocationRepository(StationRepository stationRepository, TripRepository tripRepository) {
        this.stationRepository = stationRepository;
        this.tripRepository = tripRepository;
        servicesForLocations = new HashMap<>();
    }

    @PostConstruct
    private void start() {
        logger.info("Add services for stations");
        stationRepository.getStations().forEach(station -> servicesForLocations.put(station.getLocationId(), new HashSet<>()));

        // TODO is this incorrect in the case of interchange locations ??
        tripRepository.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                filter(StopCall::callsAtStation).
                forEach(stopCall -> servicesForLocations.get(stopCall.getLocationId()).add(stopCall.getService()));
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        servicesForLocations.clear();
        logger.info("Stopped");
    }

    public Set<Service> getFor(final LocationId<?> locationId) {
        return servicesForLocations.get(locationId);
    }
}
