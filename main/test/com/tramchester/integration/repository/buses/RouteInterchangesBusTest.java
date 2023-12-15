package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

@BusTest
public class RouteInterchangesBusTest {

    private static ComponentContainer componentContainer;
    private RouteInterchangeRepository routeInterchanges;
    private StationRepository stationRepository;
    private InterchangeRepository interchangeRepository;

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
        stationRepository = componentContainer.get(StationRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);

        routeInterchanges = componentContainer.get(RouteInterchangeRepository.class);
    }

    // no longer compute cost, was inaccurate
//    @Test
//    void shouldHaveConsistencyOnZeroCostToInterchangeAndInterchanges() {
//        Set<RouteStation> zeroCostToInterchange = stationRepository.getRouteStations().stream().
//                filter(routeStation -> routeInterchanges.costToInterchange(routeStation).isZero()).
//                collect(Collectors.toSet());
//
//        Set<RouteStation> zeroCostButNotInterchange = zeroCostToInterchange.stream().
//                filter(zeroCost -> !interchangeRepository.isInterchange(zeroCost.getStation())).
//                collect(Collectors.toSet());
//
//        assertTrue(zeroCostButNotInterchange.isEmpty());
//    }

//    @Test
//    void shouldGetInterchangesForARoute() {
//
//        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.BuryManchesterAltrincham);
//
//        Set<InterchangeStation> found = routes.stream().
//                flatMap(route -> routeInterchanges.getFor(route).stream()).collect(Collectors.toSet());
//
//        assertFalse(found.isEmpty());
//    }
//
//    @Test
//    void shouldGetCostToInterchangeForRouteStation() {
//
//        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);
//
//        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
//                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());
//
//        assertFalse(navigationRoadRouteStations.isEmpty());
//
//        RouteStation navigationRoad = navigationRoadRouteStations.get(0);
//
//        int cost = routeInterchanges.costToInterchange(navigationRoad);
//
//        // cost to trafford bar
//        assertEquals(14, cost);
//
//    }
//
//    @Test
//    void shouldGetCostToInterchangeForRouteStationAdjacent() {
//
//        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);
//
//        List<RouteStation> oldTraffordRouteStations = stationRepository.getRouteStationsFor(OldTrafford.getId()).stream().
//                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());
//
//        assertFalse(oldTraffordRouteStations.isEmpty());
//
//        RouteStation oldTrafford = oldTraffordRouteStations.get(0);
//
//        int cost = routeInterchanges.costToInterchange(oldTrafford);
//
//        // cost to trafford bar
//        assertEquals(2, cost);
//
//    }
//
//    @Test
//    void shouldGetZeroCostToInterchangeForRouteStationThatIsInterchange() {
//
//        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);
//
//        List<RouteStation> cornbrookRouteStations = stationRepository.getRouteStationsFor(Cornbrook.getId()).stream().
//                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());
//
//        assertFalse(cornbrookRouteStations.isEmpty());
//
//        cornbrookRouteStations.forEach(cornbrookRoute -> {
//                    int cost = routeInterchanges.costToInterchange(cornbrookRoute);
//                    assertEquals(0, cost);
//                }
//        );
//    }
//
//    @Test
//    void shouldGetMaxCostIfNoInterchangeBetweenStationAndEndOfTheRoute() {
//        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.PiccadillyAltrincham);
//
//        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
//                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());
//
//        assertFalse(navigationRoadRouteStations.isEmpty());
//
//        navigationRoadRouteStations.forEach(routeStation -> {
//            int cost = routeInterchanges.costToInterchange(routeStation);
//            assertEquals(Integer.MAX_VALUE, cost);
//        });
//
//    }



}


