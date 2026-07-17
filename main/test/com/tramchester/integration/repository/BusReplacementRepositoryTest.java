package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.BusReplacementRepository;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
public class BusReplacementRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private BusReplacementRepository repository;
    private TramRouteHelper routeHelper;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
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
        assertTrue(repository.hasReplacementBuses());

        assertEquals(13, repository.number());
    }

    @Test
    void shouldHaveNormalTramRoute() {
        Route route = routeHelper.getPink(when);
        assertFalse(repository.isReplacement(route.getId()));
    }

    @Test
    void shouldHaveExpectedReplacementsSummer2026() {
        check("Piccadilly Station - Altrincham");
        check("Piccadilly Station - Chorlton");
        check("Piccadilly Station - The Trafford Centre");
    }

    private void check(String longName) {
        Route route = routeHelper.requireByLongName(when, longName);
        assertTrue(repository.isReplacement(route.getId()));
    }
}
