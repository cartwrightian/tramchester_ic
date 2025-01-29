package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                assertEquals(TestEnv.MetAgency(), route.getAgency(), "agency wrong" + route.getAgency());
                assertEquals(knownRoute.shortName(), route.getShortName(), "shortname " + route.getShortName());
        }
    }

    @Test
    void shouldFindUniqueCallingPointsBetween() {
        List<IdFor<Station>> stations = tramRouteHelper.getClosedBetween(OldTrafford.getId(), StPetersSquare.getId());

        assertEquals(5, stations.size());

        assertEquals(OldTrafford.getId(), stations.get(4));
        assertEquals(TraffordBar.getId(), stations.get(3));
        assertEquals(Cornbrook.getId(), stations.get(2));
        assertEquals(Deansgate.getId(), stations.get(1));
        assertEquals(StPetersSquare.getId(), stations.get(0));

    }

    @Test
    void shouldFindUniqueCallingPointsBetweenAdjacentStations() {
        List<IdFor<Station>> stations = tramRouteHelper.getClosedBetween(NavigationRoad.getId(), Timperley.getId());

        assertEquals(2, stations.size());

        assertEquals(Timperley.getId(), stations.get(0));
        assertEquals(NavigationRoad.getId(), stations.get(1));

    }

    @Test
    void shouldFindUniqueCallingPointsEndOfALine() {
        List<IdFor<Station>> stations = tramRouteHelper.getClosedBetween(Altrincham.getId(), Timperley.getId());

        assertEquals(3, stations.size());

        assertEquals(Timperley.getId(), stations.get(0));
        assertEquals(NavigationRoad.getId(), stations.get(1));
        assertEquals(Altrincham.getId(), stations.get(2));

    }

    @Test
    void shouldFailToFindUniqueSequenceIfAmbiguous() {
        assertThrows(RuntimeException.class, () -> tramRouteHelper.getClosedBetween(StPetersSquare.getId(), Victoria.getId()));
    }
}
