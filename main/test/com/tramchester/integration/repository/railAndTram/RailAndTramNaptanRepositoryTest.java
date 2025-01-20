package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.naptan.NaptanRepositoryContainer;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.integration.repository.buses.StationRepositoryBusTest.agecroftRoadStops;
import static org.junit.jupiter.api.Assertions.*;

class RailAndTramNaptanRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private NaptanRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = componentContainer.get(NaptanRepositoryContainer.class);
    }

    @Test
    void shouldContainExpectedStopsWithinLocality() {
        IdFor<Station> actoCode = TramStations.Shudehill.getId();
        assertTrue(repository.containsActo(actoCode));

        NaptanRecord naptanRecord = repository.getForActo(actoCode);
        assertEquals("Manchester City Centre", naptanRecord.getSuburb());
        assertTrue(naptanRecord.isLocalityCenter());

        IdFor<NPTGLocality> localityId = naptanRecord.getLocalityId();

        // NOTE: not Shudehill locality, meaning cannot use LocalityCode to safely associate Tram and Bus stops
        // since Shudehill bus tops are within Shudehill Locality
        assertEquals(KnownLocality.ManchesterCityCentre.getAreaId(), localityId);

        Set<NaptanRecord> withinLocality = repository.getRecordsForLocality(localityId);

        assertEquals(171, withinLocality.size(), withinLocality.toString());
    }

    @Test
    void shouldContainTrainOutOfBounds() {
        assertTrue(repository.containsTiploc(RailStationIds.LondonEuston.getId()));
    }

    @Test
    void shouldHaveAltyTrainStation() {
        IdFor<Station> altyTrainId = RailStationIds.Altrincham.getId();

        assertTrue(repository.containsTiploc(altyTrainId));
        NaptanRecord record = repository.getForTiploc(altyTrainId);

        assertEquals("Altrincham Rail Station", record.getCommonName());
        assertEquals("Altrincham", record.getTown());
        assertTrue(record.getSuburb().isEmpty());
        assertEquals(NaptanRecord.createId("9100ALTRNHM"), record.getId());

        IdFor<NPTGLocality> areaCode = record.getLocalityId();
        assertEquals(KnownLocality.Altrincham.getAreaId(), areaCode);
    }

    @Test
    void shouldHaveNaptanAreaForAltrinchamStation() {
        final IdFor<NPTGLocality> altyRailStationArea = KnownLocality.Altrincham.getAreaId();
        assertTrue(repository.containsLocality(altyRailStationArea));
        Set<NaptanRecord> inLocation = repository.getRecordsForLocality(altyRailStationArea);

        assertFalse(inLocation.isEmpty());

        Set<IdFor<NaptanRecord>> ids = inLocation.stream().map(NaptanRecord::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(NaptanRecord.createId(TramStations.Altrincham.getRawId())));
        assertTrue(ids.contains(NaptanRecord.createId(BusStations.StopAtAltrinchamInterchange.getRawId())));

    }

    @Test
    void shouldNotContainAreaOutOfBounds() {
        assertFalse(repository.containsLocality(KnownLocality.LondonWestminster.getAreaId()));
    }

    // need to use <Street xml:lang="en">Bolton Road</Street><Indicator xml:lang="en">opp</Indicator> from naptan to
    // create unique via getDisplayName
    @Test
    void shouldHaveNamesFromNaptanData() {
        NaptanRepository naptanRepository = componentContainer.get(NaptanRepository.class);

        IdSet<Station> stopIds = agecroftRoadStops.stream().map(Station::createId).collect(IdSet.idCollector());

        IdSet<Station> missing = stopIds.stream().filter(id -> !naptanRepository.containsActo(id)).collect(IdSet.idCollector());

        assertTrue(missing.isEmpty(), "missing " + missing);

        Set<String> uniqueNames = stopIds.stream().
                map(naptanRepository::getForActo).
                map(NaptanRecord::getDisplayName).collect(Collectors.toSet());

        assertEquals(4, uniqueNames.size(), "wrong number unique names " + uniqueNames);
    }


}
