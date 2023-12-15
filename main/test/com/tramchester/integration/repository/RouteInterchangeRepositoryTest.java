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
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteInterchangeRepositoryTest {

    private static ComponentContainer componentContainer;
    private RouteInterchangeRepository routeInterchanges;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
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
    void shouldHavePathToInterchangeForRouteStation() {

       Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).toList();

        assertFalse(navigationRoadRouteStations.isEmpty());

        RouteStation navigationRoad = navigationRoadRouteStations.get(0);

        boolean hasPath = routeInterchanges.hasPathToInterchange(navigationRoad);

        assertTrue(hasPath);
    }

    @Test
    void shouldHavePathToInterchangeForRouteStationAdjacent() {

        Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> oldTraffordRouteStations = stationRepository.getRouteStationsFor(OldTrafford.getId()).stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).toList();

        assertFalse(oldTraffordRouteStations.isEmpty());

        RouteStation oldTrafford = oldTraffordRouteStations.get(0);

        boolean hasPath = routeInterchanges.hasPathToInterchange(oldTrafford);

        assertTrue(hasPath);

    }

    @Test
    void shouldHavePathInterchangeForRouteStationThatIsInterchange() {

        Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> cornbrookRouteStations = stationRepository.getRouteStationsFor(Cornbrook.getId()).
                stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).
                toList();

        assertFalse(cornbrookRouteStations.isEmpty());

        cornbrookRouteStations.forEach(routeStation -> {
                    boolean hasPath = routeInterchanges.hasPathToInterchange(routeStation);
                    assertTrue(hasPath);
                }
        );
    }

}


