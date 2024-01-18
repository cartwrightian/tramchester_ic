package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.tramchester.testSupport.reference.KnownLocality.GreaterManchester;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class ValidateBusTestStations {

    private static ComponentContainer componentContainer;

    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveCorrectTestBusStations() {
        List<BusStations> testStations = Arrays.asList(BusStations.values());

        testStations.forEach(enumValue -> {
            Station testStation = enumValue.fake();
            Station realStation = enumValue.from(stationRepository);

            String testStationName = testStation.getName();
            assertEquals(realStation.getName(), testStationName, "name wrong for id: " + testStation.getId());

            // area enriched/loaded from naptan data
            //assertEquals(realStation.getArea(), testStation.getArea(),"area wrong for " + testStationName);

            assertEquals(realStation.getTransportModes(), testStation.getTransportModes(), "mode wrong for " + testStationName);
            TestEnv.assertLatLongEquals(realStation.getLatLong(), testStation.getLatLong(), 0.001,
                    "latlong wrong for " + testStationName);

            assertFalse(realStation.getDropoffRoutes().isEmpty(), "no drop offs " + realStation.getName());
            assertFalse(realStation.getPickupRoutes().isEmpty(), "no pick ups " + realStation.getName());
        });

    }

    @Test
    void shouldHaveCorrectTestCompositeStations() {

        StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

        GreaterManchester.forEach(knowLocality -> {
            StationGroup group = knowLocality.from(stationGroupsRepository);
            assertNotNull(group, "no central stations found for " + group);
            assertTrue(group.getAllContained().size()>1, "not enough stations for " + group);
        });

    }
}
