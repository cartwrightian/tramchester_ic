package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.StationBoxFactory;
import com.tramchester.geo.StationLocations;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationBoxFactoryTest {
    private static ComponentContainer componentContainer;
    private StationBoxFactory stationBoxFactory;
    private StationLocations stationLocations;
    private TramDate when;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationBoxFactory = componentContainer.get(StationBoxFactory.class);
        stationLocations = componentContainer.get(StationLocations.class);
        stationRepository = componentContainer.get(StationRepository.class);
        when = TestEnv.testDay();
    }

    @Test
    void shouldContainASingleStation() {
        BoundingBox bounds = stationLocations.getActiveStationBounds();
        int gridSize = (bounds.getMaxNorthings()-bounds.getMinNorthings()) / 100;

        final List<StationsBoxSimpleGrid> grouped = stationBoxFactory.getStationBoxes(gridSize, when);

        Station station = TramStations.StPetersSquare.from(stationRepository);

        Optional<StationsBoxSimpleGrid> findDestBox = grouped.stream().filter(box -> box.getStations().contains(station)).findFirst();
        assertTrue(findDestBox.isPresent());
    }

    @Test
    void boxesForBoundsShouldContainAllStations() {
        BoundingBox bounds = stationLocations.getActiveStationBounds();
        int gridSize = (bounds.getMaxNorthings()-bounds.getMinNorthings()) / 100;

        final List<StationsBoxSimpleGrid> grouped = stationBoxFactory.getStationBoxes(gridSize, when);

        Set<Station> allStations = stationRepository.getStationsServing(TransportMode.Tram);

        for (Station station: allStations) {
            Optional<StationsBoxSimpleGrid> findDestBox = grouped.stream().filter(box -> box.getStations().contains(station)).findFirst();
            assertTrue(findDestBox.isPresent(), "could not find a box containing " + station.getId());
        }


    }
}
