package com.tramchester.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.reference.KnowLocality;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class StationGroupTestSupport {
    private final NaptanRepository naptanRepository;
    private final StationRepository stationRepository;

    public StationGroupTestSupport(ComponentContainer componentContainer) {
        naptanRepository = componentContainer.get(NaptanRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    public StationGroup getGroupFor(KnowLocality knowLocality, boolean central, EnumSet<NaptanStopType> stopTypes) {

        IdFor<NPTGLocality> localityId = knowLocality.getId();
        if (!naptanRepository.containsArea(localityId)) {
            throw new RuntimeException("Did not find " + knowLocality + " in naptan data, is it out of bounds?");
        }

        Stream<IdFor<NaptanRecord>> actoIds = naptanRepository.getRecordsForLocality(localityId).stream().
                filter(naptanRecord -> stopTypes.contains(naptanRecord.getStopType())).
                filter(naptanRecord -> central == naptanRecord.isLocalityCenter()).
                map(NaptanRecord::getId);

        IdSet<Station> stationIds = actoIds.map(Station::createId).collect(IdSet.idCollector());

        Set<Station> stations = stationIds.stream().
                filter(stationRepository::hasStationId).
                map(stationRepository::getStationById).collect(Collectors.toSet());

        return new StationGroup(stations, localityId, format("TESTONLY_%s_%s_%s_%s", knowLocality.name(), localityId, central, stopTypes));
    }

    public StationGroup getGroupFor(KnowLocality knowLocality, String commonName) {
        IdFor<NPTGLocality> knowLocalityId = knowLocality.getId();

        if (!naptanRepository.containsArea(knowLocalityId)) {
            throw new RuntimeException("No naptan records for " + knowLocality);
        }

        Set<NaptanRecord> stops = naptanRepository.getRecordsForLocality(knowLocalityId).stream().
                filter(naptanRecord -> commonName.equalsIgnoreCase(naptanRecord.getName())).
                collect(Collectors.toSet());

        if (stops.isEmpty()) {
            throw new RuntimeException(format("Unable for find stop matching '%s' for locality %s", commonName, knowLocality));
        }

        Set<Station> stations = stops.stream().map(naptanRecord -> Station.createId(naptanRecord.getId())).
                filter(stationRepository::hasStationId).
                map(stationRepository::getStationById).
                collect(Collectors.toSet());

        if (stations.isEmpty()) {
            throw new RuntimeException(format("Unable for find stations matching '%s' for locality %s, are stops out of bounds?", stops, knowLocality));
        }

        return new StationGroup(stations, knowLocalityId, format("TESTONLY_%s_at_%s_%s", commonName, knowLocality.name(), knowLocalityId));
    }
}
