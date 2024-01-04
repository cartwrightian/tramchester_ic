package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.RouteStation;
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

import static com.tramchester.testSupport.reference.KnownTramRoute.BuryManchesterAltrincham;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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


