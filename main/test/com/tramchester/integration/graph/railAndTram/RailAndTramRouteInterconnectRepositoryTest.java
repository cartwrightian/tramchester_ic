package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.caching.DataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.search.routes.*;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.FakeDataCache;
import com.tramchester.testSupport.RailRouteHelper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteInterconnectRepositoryTest {
    private static ComponentContainer componentContainer;

    private RailRouteHelper railRouteHelper;
    private RouteIndex routeIndex;
    private int numberOfRoutes;
    private RouteInterconnectRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

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
        railRouteHelper = new RailRouteHelper(componentContainer);
        routeIndex = componentContainer.get(RouteIndex.class);
        repository = componentContainer.get(RouteInterconnectRepository.class);

        numberOfRoutes = routeRepository.numberOfRoutes();
        TramDate date = TestEnv.testDay();
    }

    @Disabled("did not find the issue, always passes")
    @Test
    void shouldHaveConsistentNumbersOnBacktracking() {

        // For tracking down bug, seems consistent for one data load, but not across multiple?

        int resultA = repository.getNumberBacktrackFor(1);

        RouteInterconnectRepository secondMatrix = createRepository();

        int resultB = secondMatrix.getNumberBacktrackFor(1);

        assertEquals(resultA, resultB);
    }

    @Test
    void shouldReproIssueGettingInterchangesAndMissingIndex() {
        Route routeA = railRouteHelper.getRoute(TrainOperatingCompanies.TP, ManchesterVictoria, Huddersfield, 1);
        Route routeB = railRouteHelper.getRoute(TrainOperatingCompanies.NT, Chester, Stockport, 1);

        RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));
        IndexedBitSet allDates = IndexedBitSet.getIdentity(numberOfRoutes, numberOfRoutes);

        PathResults results = repository.getInterchangesFor(indexPair, allDates, interchangeStation -> true);

        assertTrue(results.hasAny());

        assertEquals(3,results.getDepth());
    }

    @Test
    void shouldReproIssueGettingInterchangesAndMissingIndexExample2() {
        Route routeA = railRouteHelper.getRoute(TrainOperatingCompanies.TP, ManchesterPiccadilly, Leeds, 1);
        Route routeB = railRouteHelper.getRoute(TrainOperatingCompanies.NT, Chester, Stockport, 1);

        RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));
        IndexedBitSet allDates = IndexedBitSet.getIdentity(numberOfRoutes, numberOfRoutes);

        PathResults results = repository.getInterchangesFor(indexPair, allDates, interchangeStation -> true);

        assertTrue(results.hasAny());

        assertEquals(3,results.getDepth());
    }


    @NotNull
    private RouteInterconnectRepository createRepository() {
        NumberOfRoutes numberOfRoutes = componentContainer.get(NumberOfRoutes.class);
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        RouteIndexPairFactory pairFactory = componentContainer.get(RouteIndexPairFactory.class);
        RouteDateAndDayOverlap routeDayAndDateOverlap = componentContainer.get(RouteDateAndDayOverlap.class);
        RouteCostMatrix routeCostMatrix = componentContainer.get(RouteCostMatrix.class);

        DataCache dataCache = new FakeDataCache();

        RouteInterconnectRepository anotherRepository = new RouteInterconnectRepository(pairFactory, numberOfRoutes, routeIndex,
                interchangeRepository, routeCostMatrix, routeDayAndDateOverlap, dataCache, new GraphFilterActive(false));

        anotherRepository.start();
        return anotherRepository;
    }

}
