package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class RecentJourneysToLocations {
    private static final Logger logger = LoggerFactory.getLogger(RecentJourneysToLocations.class);

    private final LocationRepository locationRepository;

    @Inject
    public RecentJourneysToLocations(LocationRepository stationRepository) {
        this.locationRepository = stationRepository;
    }

    public Set<Location<?>> from(RecentJourneys recentJourneys, EnumSet<TransportMode> modes) {

        return recentJourneys.stream().
                map(this::getLocationFor).
                filter(Optional::isPresent).
                map(Optional::get).
                filter(location -> location.anyOverlapWith(modes)).
                collect(Collectors.toSet());
    }

    private Optional<Location<?>> getLocationFor(Timestamped record) {
        return switch (record.getLocationType()) {
            case Station, StationGroup -> getFromRepository(record);
            default -> {
                logger.warn(String.format("Unsupported location type for %s", record));
                yield Optional.empty();
            }
        };
    }

    private Optional<Location<?>> getFromRepository(Timestamped record) {
        if (locationRepository.hasLocation(record.getLocationType(), record.getId())) {
            return Optional.of(locationRepository.getLocation(record.getLocationType(), record.getId()));
        } else {
            logger.warn("Could not find location for " + record);
            return Optional.empty();
        }
    }

}
