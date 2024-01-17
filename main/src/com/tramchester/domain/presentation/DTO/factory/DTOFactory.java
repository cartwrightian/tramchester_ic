package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;
import tech.units.indriya.unit.Units;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class DTOFactory {

    // NOTE: watch out for dependencies here, e.g. Exchanges which would cause a full DB build

    @Inject
    public DTOFactory() {
    }

    public LocationRefDTO createLocationRefDTO(Location<?> location) {
        return new LocationRefDTO(location);
    }

    public LocationRefWithPosition createLocationRefWithPosition(Location<?> location) {
        return new LocationRefWithPosition(location);
    }

    public StationGroupDTO createStationGroupDTO(StationGroup stationGroup) {
        IdFor<NPTGLocality> areaId = stationGroup.getLocalityId();
        List<LocationRefWithPosition> contained = stationGroup.getContained().stream().
                map(this::createLocationRefWithPosition).collect(Collectors.toList());
        return new StationGroupDTO(areaId, contained);
    }

    public StationLinkDTO createStationLinkDTO(StationToStationConnection stationLink) {
        LocationRefWithPosition begin = createLocationRefWithPosition(stationLink.getBegin());
        LocationRefWithPosition end = createLocationRefWithPosition(stationLink.getEnd());
        Double distanceInMeters = stationLink.getDistanceInMeters().to(Units.METRE).getValue().doubleValue();
        return new StationLinkDTO(begin, end, stationLink.getLinkingModes(), distanceInMeters);
    }

    public StationNote createStationNote(Note.NoteType noteType, String text, Set<Station> seenAt) {
        List<LocationRefDTO> seenAtDTO = seenAt.stream().
                sorted(Comparator.comparing(Station::getName)).
                map(this::createLocationRefDTO).toList();
        return new StationNote(noteType, text, seenAtDTO);
    }
}
