package com.tramchester.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.naptan.NaptanStopType;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StationGroupFactoryForTests {
    private final NaptanRepository naptanRepository;
    private final StationRepository stationRepository;

    public StationGroupFactoryForTests(ComponentContainer componentContainer) {
        naptanRepository = componentContainer.get(NaptanRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    public StationGroup getGroupFor(IdFor<NPTGLocality> localityId, boolean central, EnumSet<NaptanStopType> stopTypes) {
        Stream<IdFor<NaptanRecord>> actoIds = naptanRepository.getRecordsForLocality(localityId).stream().
                filter(naptanRecord -> stopTypes.contains(naptanRecord.getStopType())).
                filter(naptanRecord -> central == naptanRecord.isLocalityCenter()).
                map(NaptanRecord::getId);

        IdSet<Station> stationIds = actoIds.map(id -> StringIdFor.convert(id, Station.class)).collect(IdSet.idCollector());

        Set<Station> stations = stationIds.stream().map(stationRepository::getStationById).collect(Collectors.toSet());

        return new StationGroup(stations, localityId, String.format("TESTONLY_%s_%s_%s", localityId, central, stopTypes));
    }
}
