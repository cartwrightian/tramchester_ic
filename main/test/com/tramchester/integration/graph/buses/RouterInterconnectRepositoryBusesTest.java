package com.tramchester.integration.graph.buses;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.search.routes.*;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.reference.KnownBusRoute.AltrinchamMacclesfield;
import static com.tramchester.testSupport.reference.KnownBusRoute.MacclesfieldAirport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
public class RouterInterconnectRepositoryBusesTest {
    private static ComponentContainer componentContainer;

    private RouteIndex routeIndex;
    private TramRouteHelper routeHelper;
    private TramDate date;
    RouteInterconnectRepository repository;
    private RouteCostMatrix routeMatrix;

    // NOTE: this test does not cause a full db rebuild, so might see VERSION node missing messages

    @BeforeAll
    static void onceBeforeAnyTestRuns() {

        TramchesterConfig tramchesterConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        ///// NOTE Clears the Cache

        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
        routeIndex = componentContainer.get(RouteIndex.class);
        date = TestEnv.testDay();
        routeMatrix = componentContainer.get(RouteCostMatrix.class);
        repository = componentContainer.get(RouteInterconnectRepository.class);
    }

    @Test
    void shouldHaveExpectedDepthWhereDirectInterchangePossible() {
        RouteIndex routeIndex = componentContainer.get(RouteIndex.class);
        Route routeA = routeHelper.getOneRoute(MacclesfieldAirport, date);
        Route routeB = routeHelper.getOneRoute(AltrinchamMacclesfield, date);


        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, EnumSet.of(Bus));
        RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));

        PathResults results = repository.getInterchangesFor(indexPair, dateOverlaps, interchangeStation -> true);

        assertTrue(results.hasAny());

        assertEquals(1, results.getDepth());
        assertEquals(1, results.numberPossible());

    }

    @Disabled("Performance testing")
    @Test
    public void shouldBuildRepository() {
        NumberOfRoutes numberOfRoutes = componentContainer.get(NumberOfRoutes.class);
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        RouteIndexPairFactory pairFactory = componentContainer.get(RouteIndexPairFactory.class);
        RouteDateAndDayOverlap routeDayAndDateOverlap = componentContainer.get(RouteDateAndDayOverlap.class);

//        DataCache dataCache = new FakeDataCache();

        RouteInterconnectRepository anotherRepository = new RouteInterconnectRepository(pairFactory, numberOfRoutes, routeIndex,
                interchangeRepository, routeMatrix, routeDayAndDateOverlap);

        for (int i = 0; i < 2; i++) {
            anotherRepository.start();
            anotherRepository.clear();
        }
    }
}
