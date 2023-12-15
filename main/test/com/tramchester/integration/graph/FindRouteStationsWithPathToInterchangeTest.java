package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.routes.FindRouteStationsWithPathToInterchange;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.BuryManchesterAltrincham;
import static com.tramchester.testSupport.reference.TramStations.Cornbrook;
import static com.tramchester.testSupport.reference.TramStations.OldTrafford;
import static org.junit.jupiter.api.Assertions.*;

public class FindRouteStationsWithPathToInterchangeTest {
    private static ComponentContainer componentContainer;
    private static Set<RouteStationId> havePaths;
    private static FindRouteStationsWithPathToInterchange finder;
    private TramRouteHelper tramRouteHelper;
    private TramDate when;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        finder = componentContainer.get(FindRouteStationsWithPathToInterchange.class);
        havePaths = finder.havePathToInterchange();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);
        stationRepository = componentContainer.get(StationRepository.class);
        when = TestEnv.testDay();

    }

    @Test
    void shouldHavePathToInterchangeForAllTramStations() {
        // this might not be the case for bus or train
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        Set<RouteStation> allRouteStations = stationRepository.getRouteStations().
                stream().filter(routeStation -> !interchangeRepository.isInterchange(routeStation.getStationId())).
                collect(Collectors.toSet());

        assertEquals(havePaths.size(), allRouteStations.size());
    }

    @Test
    void shouldHaveExpectedNumberOfResultsForARoute() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.EcclesManchesterAshtonUnderLyne, when);

        Set<RouteStationId> havePaths;
        try(MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            havePaths = finder.havePathToInterchange(txn, route);
        }

        Set<RouteStation> notInterchangesOnRoute = stationRepository.getRouteStations().stream().
                filter(routeStation -> routeStation.getRoute().equals(route)).
                filter(routeStation -> !interchangeRepository.isInterchange(routeStation.getStationId())).
                collect(Collectors.toSet());

        assertEquals(notInterchangesOnRoute.size(), havePaths.size());

    }

    @Test
    void shouldGetCostToAnInterchangeForRouteStation() {

        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, when);
        Station naviRoad = TramStations.NavigationRoad.from(stationRepository);

        RouteStation routeStation = stationRepository.getRouteStation(naviRoad, route);

        assertTrue(havePaths.contains(routeStation.getId()));
    }

    @Test
    void shouldGetCostToInterchangeForRouteStationAdjacent() {

        Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> oldTraffordRouteStations = stationRepository.getRouteStationsFor(OldTrafford.getId()).stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).toList();

        assertFalse(oldTraffordRouteStations.isEmpty());

        RouteStation oldTrafford = oldTraffordRouteStations.get(0);

        assertTrue(havePaths.contains(oldTrafford.getId()));

    }

    @Test
    void shouldGetNotResultsIfInterchange() {

        Route route = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        List<RouteStation> cornbrookRouteStations = stationRepository.getRouteStationsFor(Cornbrook.getId()).
                stream().
                filter(routeStation -> route.equals(routeStation.getRoute())).
                toList();

        assertFalse(cornbrookRouteStations.isEmpty());

        long cornBrookinterchangeCosts = havePaths.stream().
                filter(routeStationId -> routeStationId.getStationId().equals(Cornbrook.getId())).count();

        assertEquals(0L, cornBrookinterchangeCosts, "cornbrook is an interchange");

    }

}
