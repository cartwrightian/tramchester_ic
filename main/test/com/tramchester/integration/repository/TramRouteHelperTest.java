package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Test
    void shouldFindAllKnownRoutes() {

        TramDate date = TestEnv.testDay();

        Set<TFGMRouteNames> missing = Arrays.stream(TFGMRouteNames.values()).
                filter(routeNames -> !routeNames.isReplacementBus()).
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
        Set<Route> found = routeRepository.findRoutesByShortName(MutableAgency.METL, routeName.getShortName());
        assertFalse(found.isEmpty(), "Found no routes to match " + routeName);
        List<Route> onDate = found.stream().filter(route -> route.isAvailableOn(date)).toList();
        assertFalse(onDate.isEmpty(), "None matched date " + date + " " + HasId.asIds(found));
        return onDate;
//       return routeRepository.getRoutesRunningOn(date, TramsOnly).stream().
//                filter(route -> ((TramRouteId) route.getId()).getRouteName() == routeName).
//                toList();
    }


}
