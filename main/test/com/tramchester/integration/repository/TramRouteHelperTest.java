package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TramRouteHelperTest {

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

    @Test
    void shouldFindAllKnownRoutes() {

        TramDate date = TramDate.from(TestEnv.LocalNow());

        Set<KnownTramRoute> knownRoutes = KnownTramRoute.getFor(date);

        for(KnownTramRoute knownRoute : knownRoutes) {
            Route route = tramRouteHelper.getOneRoute(knownRoute, date);
//            assertFalse(found.isEmpty(),"missing " + knownRoute.toString());
//            found.forEach(route -> {
                assertEquals(TestEnv.MetAgency(), route.getAgency(), "agency wrong" + route.getAgency());
                assertEquals(knownRoute.shortName(), route.getShortName(), "shortname " + route.getShortName());
//            });
        }
    }
}
