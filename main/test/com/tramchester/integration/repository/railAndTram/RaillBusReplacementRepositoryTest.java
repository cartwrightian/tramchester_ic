package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.BusReplacementRepository;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RaillBusReplacementRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private BusReplacementRepository repository;
    private TramRouteHelper routeHelper;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        RailAndTramGreaterManchesterConfig tramchesterConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        repository = componentContainer.get(BusReplacementRepository.class);
        routeHelper = new TramRouteHelper(componentContainer);

        when = TestEnv.testDay();
    }

    @Test
    void shouldHaveExpectedNumberOfReplacementBuses() {
        assertFalse(repository.hasReplacementBuses(), "buses running");

        assertEquals(7, repository.number(), repository.getReplacementBuses().toString());
    }


    private void check(String longName) {
        Route route = routeHelper.requireByLongName(when, longName);
        assertTrue(repository.isReplacement(route.getId()));
    }
}
