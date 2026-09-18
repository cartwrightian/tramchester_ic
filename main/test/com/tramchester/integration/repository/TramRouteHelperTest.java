package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static org.junit.jupiter.api.Assertions.*;

class TramRouteHelperTest {

    private static GuiceContainerDependencies componentContainer;
    private RouteRepository routeRepository;

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
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldCheckBusRoutes() {
        assertTrue(TFGMRouteNames.ReplacementBus.isReplacementBus());
    }

    @DisabledUntilDate(year = 2026, month = 9, day = 26)
    @Test
    void shouldFindAllKnownRoutes() {

        TramDate date = TestEnv.testDay();

        Set<TFGMRouteNames> missing = Arrays.stream(TFGMRouteNames.values()).
                filter(routeName -> getRouteByName(date, routeName).isEmpty()).
                collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), missing, "Missing on " + date);

        for(TFGMRouteNames routeName : TFGMRouteNames.values()) {
            if (!routeName.isReplacementBus()) {
                getRouteByName(date, routeName).forEach(route ->
                        assertEquals(routeName.getShortName(), route.getShortName(), "shortname "
                                + route.getShortName()));

            }
        }
    }

    public List<Route> getRouteByName(TramDate date, TFGMRouteNames routeName) {
       return routeRepository.getRoutesRunningOn(date, TramsOnly).stream().
                filter(route -> ((TramRouteId) route.getId()).getRouteName() == routeName).
                toList();
    }


}
