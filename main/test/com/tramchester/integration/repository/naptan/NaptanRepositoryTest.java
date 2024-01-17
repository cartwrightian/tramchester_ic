package com.tramchester.integration.repository.naptan;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.naptan.NaptanRepositoryContainer;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.KnowLocality;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.integration.repository.buses.StationRepositoryBusTest.agecroftRoadStops;
import static org.junit.jupiter.api.Assertions.*;

class NaptanRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private NaptanRepository respository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan(
                EnumSet.of(TransportMode.Bus, TransportMode.Tram, TransportMode.Train));
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        respository = componentContainer.get(NaptanRepositoryContainer.class);
    }

    @Test
    void shouldContainExpectedStopsWithinLocality() {
        IdFor<Station> actoCode = TramStations.Shudehill.getId();
        assertTrue(respository.containsActo(actoCode));

        NaptanRecord data = respository.getForActo(actoCode);
        assertEquals("Manchester City Centre", data.getSuburb());

        IdFor<NPTGLocality> localityId = data.getLocalityId();

        Set<NaptanRecord> withinLocality = respository.getRecordsForLocality(localityId);

        assertEquals(168, withinLocality.size(), withinLocality.toString());
    }

    @Test
    void shouldContainBusStopWithinArea() {
        IdFor<Station> actoCode = BusStations.ManchesterAirportStation.getId();
        assertTrue(respository.containsActo(actoCode));

        NaptanRecord record = respository.getForActo(actoCode);
        assertEquals("Manchester Airport", record.getSuburb());

        final IdFor<NPTGLocality> areaCode = record.getLocalityId();
        assertEquals(KnowLocality.ManchesterAirport.getId(), areaCode);
    }

    @Test
    void shouldNotContainTrainOutOfBounds() {
        assertFalse(respository.containsTiploc(RailStationIds.LondonEuston.getId()));
    }

    @Test
    void shouldHaveDataForTrainStation() {
        IdFor<Station> tiploc = RailStationIds.Macclesfield.getId();

        assertTrue(respository.containsTiploc(tiploc));

        NaptanRecord record = respository.getForTiploc(tiploc);
        assertEquals(record.getCommonName(), "Macclesfield Rail Station");
        assertEquals(record.getTown(), "Macclesfield");
        assertTrue(record.getSuburb().isEmpty());
        assertEquals(record.getId(), NaptanRecord.createId("9100MACLSFD"));

        final IdFor<NPTGLocality> areaCode = record.getLocalityId();

        assertEquals(KnowLocality.Macclesfield.getId(), areaCode);
    }

    @Test
    void shouldHaveAltyTrainStation() {
        IdFor<Station> altyTrainId = RailStationIds.Altrincham.getId();

        assertTrue(respository.containsTiploc(altyTrainId));
        NaptanRecord record = respository.getForTiploc(altyTrainId);

        assertEquals("Altrincham Rail Station", record.getCommonName());
        assertEquals("Altrincham", record.getTown());
        assertTrue(record.getSuburb().isEmpty());
        assertEquals(NaptanRecord.createId("9100ALTRNHM"), record.getId());

        IdFor<NPTGLocality> areaCode = record.getLocalityId();
        assertEquals(KnowLocality.Altrincham.getId(), areaCode);
    }

    @Test
    void shouldHaveNaptanAreaForAltrinchamStation() {
        final IdFor<NPTGLocality> altyRailStationArea = KnowLocality.Altrincham.getId();
        assertTrue(respository.containsArea(altyRailStationArea));
        Set<NaptanRecord> inLocation = respository.getRecordsForLocality(altyRailStationArea);

        assertFalse(inLocation.isEmpty());

        Set<IdFor<NaptanRecord>> ids = inLocation.stream().map(NaptanRecord::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(NaptanRecord.createId(TramStations.Altrincham.getRawId())));
        assertTrue(ids.contains(NaptanRecord.createId(BusStations.StopAtAltrinchamInterchange.getRawId())));

    }

    @Test
    void shouldNotContainAreaOutOfBounds() {
        assertFalse(respository.containsArea(KnowLocality.LondonWestminster.getId()));
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

    @Test
    void shouldFindAllTestBusStations() {
        for(BusStations station :BusStations.values()) {
            IdFor<Station> actoCode = station.getId();
            assertTrue(respository.containsActo(actoCode), "missing for " + station);
            NaptanRecord fromNaptan = respository.getForActo(actoCode);
            assertEquals(station.getName(), fromNaptan.getDisplayName(), fromNaptan.toString());
        }
    }


    @Test
    void shouldGetBoundary() {

        List<LatLong> points = respository.getBoundaryFor(KnowLocality.Shudehill.getId());

        assertEquals(8, points.size());
    }

    @Test
    void shouldFindKnutsfordLocalityStations() {

        final IdFor<Station> stopId = BusStations.KnutsfordStationStand3.getId();
        NaptanRecord fromNaptan = respository.getForActo(stopId);
        assertNotNull(fromNaptan);

        IdFor<NPTGLocality> locality = fromNaptan.getLocalityId();
        assertEquals(KnowLocality.Knutsford.getId(), locality);

        Set<NaptanRecord> allRecordsForArea = respository.getRecordsForLocality(locality);

        assertEquals(46, allRecordsForArea.size(), allRecordsForArea.toString());

        Set<NaptanRecord> central = allRecordsForArea.stream().filter(NaptanRecord::isLocalityCenter).collect(Collectors.toSet());

        assertEquals(8, central.size());

        Set<NaptanRecord> busStationStops = central.stream().
                filter(naptanRecord -> naptanRecord.getStopType().equals(NaptanStopType.busCoachTrolleyStationBay)).
                collect(Collectors.toSet());

        assertEquals(3, busStationStops.size());

        busStationStops.forEach(busStationRecord -> {
            assertEquals("Bus Station", busStationRecord.getCommonName(), "wrong name for " + busStationRecord);
            assertEquals("Knutsford", busStationRecord.getTown(), "wrong town for " + busStationRecord);
            assertEquals("", busStationRecord.getSuburb(), "wrong suburb for " + busStationRecord);

            assertTrue(busStationRecord.getIndicator().startsWith("Stand"));

//            assertEquals("Bus Station", naptanRecord.getDisplayName(), "wrong name for " + naptanRecord);

        });
    }

}
