package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BusTest
class BusStationsLocationsTest {
    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;
    private static EnumSet<TransportMode> modes;

    private StationLocations stationLocations;
    private MarginInMeters inMeters;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        modes = testConfig.getTransportModes();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationLocations = componentContainer.get(StationLocations.class);
        stationRepository = componentContainer.get(StationRepository.class);
        inMeters =  MarginInMeters.ofKM(testConfig.getNearestStopForWalkingRangeKM());
    }

    @AfterAll
    static void afterEachTestRuns() {
        componentContainer.close();
    }

    //
    // these tests here to support tuning config parameters for num and distance of stations
    //

    @Test
    void shouldGetAllStationsCloseToPiccGardens() {
        List<Station> result = stationLocations.nearestStationsSorted(TestPostcodes.NearPiccadillyGardens, 500,
                inMeters, modes);
        assertEquals(50, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralBury() {
        List<Station> result = stationLocations.nearestStationsSorted(TestPostcodes.CentralBury, 500,
                inMeters, modes);
        assertEquals(40, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralAlty() {
        List<Station> result = stationLocations.nearestStationsSorted(BusStations.StopAtAltrinchamInterchange.from(stationRepository),
                500, inMeters, modes);
        assertEquals(18, result.size());
    }

}
