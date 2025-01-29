package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.UpcomingDates;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.Eccles;
import static com.tramchester.testSupport.reference.TramStations.MediaCityUK;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpcomingDatesTest {
    private static GuiceContainerDependencies componentContainer;
    private TramRouteHelper tramRouteHelper;

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
    void beforeEachTestRuns() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);
    }

    @Disabled("WIP")
    @Test
    void shouldHaveExpectedClosures() {
        List<IdFor<Station>> expectedClosed = tramRouteHelper.getClosedBetween(Eccles.getId(), MediaCityUK.getId());

        DateRange range = UpcomingDates.MediaCityEcclesWorks2025;

        range.stream().forEach(date -> {
            IdSet<Station> missing = expectedClosed.stream().filter(closed -> !UpcomingDates.hasClosure(closed, date)).
                    collect(IdSet.idCollector());
            assertTrue(missing.isEmpty(), "On " + date + " still open " + missing);
        });


    }
}
