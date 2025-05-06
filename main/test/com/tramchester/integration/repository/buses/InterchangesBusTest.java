package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class InterchangesBusTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository interchangeRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldFindBusInterchanges() {
        IdSet<Station> interchanges = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).
                collect(IdSet.idCollector());
        assertFalse(interchanges.isEmpty());

        assertFalse(interchanges.contains(StockportAtAldi.getId()));
        assertFalse(interchanges.contains(StockportNewbridgeLane.getId()));

        assertTrue(interchanges.contains(StopAtAltrinchamInterchange.getId()));

    }

    @Disabled("route no longer exists")
    @Test
    void shouldNotCountLinksForSameRoute() {
        // Was here to diagnose issues with automatic id of interchanges
        Route route = routeRepository.getRouteById(Route.createBasicRouteId("SCMN149A:O:CURRENT"));

        assertNotNull(route);

        Set<InterchangeStation> interForRoutes = interchangeRepository.getAllInterchanges().stream().
                filter(interchangeStation -> interchangeStation.getPickupRoutes().contains(route)).collect(Collectors.toSet());

        assertEquals(18, interForRoutes.size(), "too many interchanges " + interForRoutes);
    }

    @Test
    void shouldAllBeSingleModeForBus() {
        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        interchanges.forEach(interchangeStation -> assertFalse(interchangeStation.isMultiMode(), interchangeStation.toString()));
    }

    // TODO Reinstate
//    @Test
//    void shouldHaveReachableInterchangeForEveryRoute() {
//        Set<Route> routesWithInterchanges = RoutesWithInterchanges(interchangeRepository, Bus);
//        Set<Route> all = routeRepository.getRoutes();
//
//        // Note works for 2 links, not for 3 links
//        assertEquals(all, routesWithInterchanges);
//    }



}
