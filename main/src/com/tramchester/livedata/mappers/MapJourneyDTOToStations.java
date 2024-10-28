package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@LazySingleton
public class MapJourneyDTOToStations {
    private static final Logger logger = LoggerFactory.getLogger(MapJourneyDTOToStations.class);

    public IdSet<Station> getAllChangeStations(final List<JourneyDTO> journeys) {
        // filter out walks here as causes issues with Nearest Station
        return journeys.stream().
                flatMap(journeyDTO -> journeyDTO.getChangeStations().stream()).
                filter(ref -> ref.getLocationType() == LocationType.Station).
                map(LocationRefDTO::getId).
                map(id -> Station.createId(id.getActualId())).collect(IdSet.idCollector());
    }

    public IdFor<Station> getFinalStationId(final List<JourneyDTO> journeyDTOS) {
        // last station for each journey, right now expect all to be the same, but TODO
        IdSet<Station> unique = journeyDTOS.stream().
                map(JourneyDTO::getPath).
                map(this::lastStationIn).
                map(LocationRefDTO::getId).
                map(Station::createId).
                collect(IdSet.idCollector());

        List<IdFor<Station>> stations = unique.toList();

        if (stations.isEmpty()) {
            logger.error("Could not find any final destinations from journeys " + journeyDTOS);
            return IdFor.invalid(Station.class);
        }

        if (stations.size()==1) {
            return stations.get(0);
        }

        // TODO is it ok just pick one?
        logger.warn("Found multiple final (will use first one) stations " + stations + " for " + journeyDTOS);
        return stations.get(0);
    }

    private LocationRefWithPosition lastStationIn(final List<LocationRefWithPosition> path) {
        for (int i = path.size()-1; i >= 0; i--) {
            final LocationRefWithPosition location = path.get(i);
            if (location.getLocationType()==LocationType.Station) {
                return location;
            }
        }
        throw new RuntimeException("Failed to find a stations in " + path);
    }
}
