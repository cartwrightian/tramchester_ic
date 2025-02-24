package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.*;
import com.tramchester.repository.postcodes.PostcodeRepository;

import jakarta.inject.Inject;

@LazySingleton
public class LocationRepository {
    //private static final Logger logger = LoggerFactory.getLogger(LocationRepository.class);

    private final StationRepository stationRepository;
    private final StationGroupsRepository stationGroupsRepository;
    private final PostcodeRepository postcodeRepository;

    @Inject
    public LocationRepository(StationRepository stationRepository, StationGroupsRepository stationGroupsRepository,
                              PostcodeRepository postcodeRepository) {
        this.stationRepository = stationRepository;
        this.stationGroupsRepository = stationGroupsRepository;
        this.postcodeRepository = postcodeRepository;
    }

    public boolean hasLocation(LocationType type, IdForDTO idForDTO) {
        String rawId = idForDTO.getActualId();
        return switch (type) {
            case Station -> stationRepository.hasStationId(Station.createId(rawId));
            case Platform, MyLocation -> false;
            case StationGroup -> stationGroupsRepository.hasGroup(StationLocalityGroup.createId(rawId));
            case Postcode -> postcodeRepository.hasPostcode(PostcodeLocation.createId(rawId));
        };
    }

    public Location<?> getLocation(final LocationType type, final IdForDTO idForDTO) {
        final String rawId = idForDTO.getActualId();
        return switch (type) {
            case Station -> stationRepository.getStationById(Station.createId(rawId));
            case Platform -> throw new RuntimeException("Not supported yet");
            case StationGroup -> stationGroupsRepository.getStationGroup(StationLocalityGroup.createId(rawId));
            case Postcode -> postcodeRepository.getPostcode(PostcodeLocation.createId(rawId));
            case MyLocation -> MyLocation.parseFromId(rawId);
        };
    }

    public Location<?> getLocation(final IdFor<? extends Location<?>> locationId) {
        final LocationType type = LocationType.getFor(locationId);
        return switch (type) {
            case Station -> getStation(locationId);
            case StationGroup -> getGroupStation(locationId);
            case Postcode, MyLocation, Platform -> throw new RuntimeException("not supported yet");
        };
    }

    private <T extends Location<?>> Location<?> getStation(IdFor<T> location) {
        IdFor<Station> stationId = StringIdFor.convert(location, Station.class);
        return stationRepository.getStationById(stationId);
    }

    private <T extends Location<?>> Location<?> getGroupStation(IdFor<T> location) {
        IdFor<StationLocalityGroup> stationId = StringIdFor.convert(location, StationLocalityGroup.class);
        return stationGroupsRepository.getStationGroup(stationId);
    }

}
