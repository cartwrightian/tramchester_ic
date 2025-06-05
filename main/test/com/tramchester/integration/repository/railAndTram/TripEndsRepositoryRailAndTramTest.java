package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.TripEndsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("wip")
@GMTest
class TripEndsRepositoryRailAndTramTest {
    private static ComponentContainer componentContainer;
    private TripEndsRepository endStationsRepository;

    // TODO this code is only used for test support

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        endStationsRepository = componentContainer.get(TripEndsRepository.class);
    }

    @Test
    void shouldFindEndsOfLinesForTrain() {
        IdSet<Station> results = endStationsRepository.getStations(TransportMode.Train);

        assertFalse(results.isEmpty());

        assertTrue(results.contains(RailStationIds.ManchesterVictoria.getId()), results.toString());
        assertTrue(results.contains(RailStationIds.ManchesterPiccadilly.getId()), results.toString());
        assertFalse(results.contains(RailStationIds.NavigationRaod.getId()), results.toString());
    }

    @Test
    void shouldFindEndsOfLinesForTram() {
        IdSet<Station> results = endStationsRepository.getStations(TransportMode.Tram);

        assertFalse(results.isEmpty());

        assertTrue(results.contains(Victoria.getId()), results.toString());
        assertTrue(results.contains(ManAirport.getId()), results.toString());

        assertTrue(results.contains(Rochdale.getId()));

        IdSet<Station> missing = getEndOfTheLine().stream().map(TramStations::getId).
                filter(eolId -> !results.contains(eolId)).
                collect(IdSet.idCollector());

        assertTrue(missing.isEmpty(), "missing " + missing);
    }


}
