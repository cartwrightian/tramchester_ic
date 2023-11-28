package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteInterchangeRepositoryTest {

    private static ComponentContainer componentContainer;
    private RouteInterchangeRepository routeInterchanges;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
    private InterchangeRepository interchangeRepository;
    private TramDate when;

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
        stationRepository = componentContainer.get(StationRepository.class);
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);

        routeInterchanges = componentContainer.get(RouteInterchangeRepository.class);

        when = TestEnv.testDay();

    }

    @Test
    void shouldGetInterchangesForARoute() {

        Route buryToAlty = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        Set<InterchangeStation> interchangesForRoute = routeInterchanges.getFor(buryToAlty);

        IdSet<Station> stations = interchangesForRoute.stream().
                map(InterchangeStation::getStationId).
                collect(IdSet.idCollector());

        assertFalse(stations.isEmpty());

        assertTrue(stations.contains(Cornbrook.getId()));
        assertTrue(stations.contains(TraffordBar.getId()));
        assertTrue(stations.contains(StPetersSquare.getId()));
        assertTrue(stations.contains(Victoria.getId()));
        assertTrue(stations.contains(Deansgate.getId()));

    }

    @Test
    void shouldHaveExpectedRoutesAtCornbrook() {
        Route buryToAlty = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        Set<InterchangeStation> interchangesForRoute = routeInterchanges.getFor(buryToAlty);
        Optional<InterchangeStation> maybeCornbook = interchangesForRoute.stream().
                filter(interchangeStation -> interchangeStation.getStationId().equals(Cornbrook.getId())).
                findFirst();

        assertTrue(maybeCornbook.isPresent());

        InterchangeStation cornbrook = maybeCornbook.get();

        TramDate date = TestEnv.testDay();

        Set<Route> cornbrookPickups = cornbrook.getPickupRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
        Set<Route> cornbrookDropofss = cornbrook.getDropoffRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        int throughRoutes = 6; // might not match the map, which includes psuedo-routes that are made of trams running part of an existing route
        assertEquals(throughRoutes  , cornbrookPickups.size(), HasId.asIds(cornbrookPickups));
        assertEquals(throughRoutes , cornbrookDropofss.size(), HasId.asIds(cornbrookDropofss));

        assertTrue(cornbrookPickups.contains(buryToAlty));
        assertTrue(cornbrookDropofss.contains(buryToAlty));

//        Route altyToBury = tramRouteHelper.getOneRoute(AltrinchamManchesterBury, when);

        assertTrue(cornbrookPickups.contains(buryToAlty));
        assertTrue(cornbrookDropofss.contains(buryToAlty));

        Route toEccles = tramRouteHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);

        assertTrue(cornbrookPickups.contains(toEccles));
        assertTrue(cornbrookDropofss.contains(toEccles));

        Route toTraffordCenter = tramRouteHelper.getOneRoute(CornbrookTheTraffordCentre, when);

        assertTrue(cornbrookPickups.contains(toTraffordCenter));

        Route victoriaToAirport = tramRouteHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, when);

        assertTrue(cornbrookPickups.contains(victoriaToAirport));
        assertTrue(cornbrookDropofss.contains(victoriaToAirport));

    }

    @Test
    void shouldGetCostToInterchangeForRouteStation() {

       Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).toList();

        assertFalse(navigationRoadRouteStations.isEmpty());

        RouteStation navigationRoad = navigationRoadRouteStations.get(0);

        Duration cost = routeInterchanges.costToInterchange(navigationRoad);

        // cost to trafford bar
        assertMinutesEquals(14, cost);
    }

    @Test
    void shouldGetCostToInterchangeForRouteStationAdjacent() {

        Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> oldTraffordRouteStations = stationRepository.getRouteStationsFor(OldTrafford.getId()).stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).toList();

        assertFalse(oldTraffordRouteStations.isEmpty());

        RouteStation oldTrafford = oldTraffordRouteStations.get(0);

        Duration cost = routeInterchanges.costToInterchange(oldTrafford);

        // cost to trafford bar
        assertMinutesEquals(2, cost);

    }

    @Test
    void shouldGetZeroCostToInterchangeForRouteStationThatIsInterchange() {

        Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> cornbrookRouteStations = stationRepository.getRouteStationsFor(Cornbrook.getId()).
                stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).
                toList();

        assertFalse(cornbrookRouteStations.isEmpty());

        cornbrookRouteStations.forEach(routeStation -> {
                    Duration cost = routeInterchanges.costToInterchange(routeStation);
                    assertTrue(cost.isZero());
                }
        );
    }

    @Disabled("no longer applies with new route data")
    @Test
    void shouldGetMaxCostIfNoInterchangeBetweenStationAndEndOfTheRoute() {

        Route towardsEndOfEnd = tramRouteHelper.getOneRoute(KnownTramRoute.VictoriaWythenshaweManchesterAirport, when);

        List<RouteStation> peelHallRouteStations = stationRepository.getRouteStationsFor(PeelHall.getId()).stream().
                filter(routeStation -> routeStation.getRoute().equals(towardsEndOfEnd)).toList();

        assertFalse(peelHallRouteStations.isEmpty());

        peelHallRouteStations.forEach(routeStation -> {
            Duration cost = routeInterchanges.costToInterchange(routeStation);
            assertTrue(cost.isNegative());
        });

    }

    @Test
    void shouldHaveConsistencyOnZeroCostToInterchangeAndInterchanges() {
        Set<RouteStation> zeroCostToInterchange = stationRepository.getRouteStations().stream().
                filter(routeStation -> routeInterchanges.costToInterchange(routeStation).isZero()).
                collect(Collectors.toSet());

        Set<RouteStation> zeroCostButNotInterchange = zeroCostToInterchange.stream().
                filter(zeroCost -> !interchangeRepository.isInterchange(zeroCost.getStation())).
                collect(Collectors.toSet());

        assertTrue(zeroCostButNotInterchange.isEmpty(), zeroCostButNotInterchange.toString());
    }



}


