package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteEndRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteEndRepositoryTramTest {
    private static ComponentContainer componentContainer;
    private RouteEndRepository endStationsRepository;

    // TODO this code is only used for test support

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        endStationsRepository = componentContainer.get(RouteEndRepository.class);
    }

    @Test
    void shouldFindEndsOfLinesForTram() {
        IdSet<Station> results = endStationsRepository.getStations(TransportMode.Tram);

        assertFalse(results.isEmpty());

        assertTrue(results.contains(Victoria.getId()), results.toString());
        assertTrue(results.contains(ManAirport.getId()), results.toString());

        // rochdale closed
        assertTrue(results.contains(Rochdale.getId()));

        IdSet<Station> missing = getEndOfTheLine().stream().map(TramStations::getId).
                filter(this::workaroundPiccGardensStillClosedInTimetable).
                filter(eolId -> !results.contains(eolId)).
                collect(IdSet.idCollector());

        assertTrue(missing.isEmpty(), "missing " + missing);

        // TODO This code only used to test support
        //assertEquals(eolIds.size() , results.size(), results.toString());
    }

    private boolean workaroundPiccGardensStillClosedInTimetable(IdFor<Station> id) {
        return !id.equals(PiccadillyGardens.getId());
    }

}
