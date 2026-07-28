package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationGroup;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.SimpleStageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.LocationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


// TODO rename

@LazySingleton
public class MapJourneyDTOToStations {
    private static final Logger logger = LoggerFactory.getLogger(MapJourneyDTOToStations.class);

    private final LocationRepository locationRepository;

    @Inject
    public MapJourneyDTOToStations(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public static ImmutableIdSet<Station> getAllChangeStations(final List<JourneyDTO> journeys) {
        // filter out walks here as causes issues with Nearest Station
        return journeys.stream().
                flatMap(journeyDTO -> journeyDTO.getChangeStations().stream()).
                filter(ref -> ref.getLocationType() == LocationType.Station).
                map(LocationRefDTO::getId).
                map(id -> Station.createId(id.getActualId())).collect(IdSet.idCollector());
    }

    public static IdFor<Station> getFinalStationId(final List<JourneyDTO> journeyDTOS) {
        // last station for each journey, right now expect all to be the same, but TODO
        IdSet<Station> unique = journeyDTOS.stream().
                map(JourneyDTO::getPath).
                map(MapJourneyDTOToStations::lastStationIn).
                map(LocationRefDTO::getId).
                map(Station::createId).
                collect(IdSet.idCollector());

        List<IdFor<Station>> stations = unique.toList();

        if (stations.isEmpty()) {
            logger.error("Could not find any final destinations from journeys " + journeyDTOS);
            return IdFor.invalid(Station.class);
        }

        if (stations.size()==1) {
            return stations.getFirst();
        }

        // TODO is it ok just pick one?
        logger.warn("Found multiple final (will use first one) stations " + stations + " for " + journeyDTOS);
        return stations.getFirst();
    }

    private static LocationRefWithPosition lastStationIn(final List<LocationRefWithPosition> path) {
        for (int i = path.size()-1; i >= 0; i--) {
            final LocationRefWithPosition location = path.get(i);
            if (location.getLocationType()==LocationType.Station) {
                return location;
            }
        }
        throw new RuntimeException("Failed to find a stations in " + path);
    }

    public DestinationAndCallingPoints getDestAndCalling(final List<JourneyDTO> journeys) {
        final ImmutableIdSet<Station> changeStations = getAllChangeStations(journeys);
        final IdFor<Station> finalStation = getFinalStationId(journeys);
        return new DestinationAndCallingPoints(finalStation, changeStations);
    }

    public StationGroup getDepartureLocations(final List<JourneyDTO> journeys) {

        final List<Station> locations = journeys.stream().
                flatMap(journey -> getDepartureLocations(journey).stream()).
                toList();

        return new StationGroup(new LocationSet<>(locations), "departures",
                LatLong.Invalid, NPTGLocality.InvalidId());
    }


    // TODO Test Scenarios
    private List<Station> getDepartureLocations(final JourneyDTO journey) {
        final List<Station> result = new ArrayList<>();
        if (journey.getBegin().getLocationType()==LocationType.Station) {
            final Station journeyBegin = (Station) locationRepository.getLocation(journey.getBegin());
            result.add(journeyBegin);
        } else {
            logger.info("Journey begin is not a station, was" + journey.getBegin());
        }

        final List<SimpleStageDTO> stages = journey.getStages();
        if (stages.size()>=2) {
            final SimpleStageDTO firstStage = stages.getFirst();
            if (firstStage.getMode()==TransportMode.Connect || firstStage.getMode()==TransportMode.Walk) {
                if (firstStage.getLastStation().getLocationType()==LocationType.Station) {
                    final Station location = (Station) locationRepository.getLocation(firstStage.getLastStation());
                    result.add(location);
                } else {
                    logger.warn("Destination of first stage not a station " + firstStage);
                }
            }
        }
        return result;
    }
}
