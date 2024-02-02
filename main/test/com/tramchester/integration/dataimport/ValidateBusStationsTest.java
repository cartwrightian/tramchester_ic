package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@BusTest
class ValidateBusStationsTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private StationRepository stationRepository;
    private StationGroupsRepository stationGroupRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        stationGroupRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @Test
    void shouldHaveCorrectTestBusStations() {
        List<BusStations> testStations = Arrays.asList(BusStations.values());

        testStations.forEach(enumValue -> {
            assertTrue(stationRepository.hasStationId(enumValue.getId()), enumValue + " is missing from repo");

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
    void shouldHaveStationInBoundsLatLong() {
        BoundingBox bounds = config.getBounds();

        Set<BusStations> outOfBounds = Arrays.stream(BusStations.values()).
                filter(station -> !bounds.contained(station.getLatLong())).
                collect(Collectors.toSet());

        assertTrue(outOfBounds.isEmpty(), "had stations out of bounds " + outOfBounds + " for box " + bounds + " co-ords " + displayCoord(outOfBounds));
    }

    private String displayCoord(Set<BusStations> outOfBounds) {
        StringBuilder result = new StringBuilder();
        for (BusStations station : outOfBounds) {
            if (!result.isEmpty()) {
                result.append(" ,");
            }
            LatLong latLong = station.getLatLong();
            result.append(latLong);
            result.append(" ").append(CoordinateTransforms.getGridPosition(latLong));
        }
        return result.toString();
    }

    @Test
    void shouldHaveCorrespondingLocalityLoadedForTestStops() {
        // only checks for those loaded, see above test for catching missing ones
        Set<Station> missingALocality = Arrays.stream(BusStations.values()).
                filter(station -> stationRepository.hasStationId(station.getId())).
                map(station -> station.from(stationRepository)).
                filter(station -> !stationGroupRepository.hasArea(station.getLocalityId())).
                collect(Collectors.toSet());

        assertTrue(missingALocality.isEmpty(), missingALocality.toString());
    }


}
