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
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.search.routes.*;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.FakeDataCache;
import com.tramchester.testSupport.RailRouteHelper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteInterconnectRepositoryTest {
    private static ComponentContainer componentContainer;

    private RailRouteHelper railRouteHelper;
    private RouteIndex routeIndex;
    private int numberOfRoutes;
    private RouteInterconnectRepository repository;
    private RouteRepository routeRepository;

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
        routeRepository = componentContainer.get(RouteRepository.class);
        railRouteHelper = new RailRouteHelper(componentContainer);
        routeIndex = componentContainer.get(RouteIndex.class);
        repository = componentContainer.get(RouteInterconnectRepository.class);

        numberOfRoutes = routeRepository.numberOfRoutes();
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
        Set<Route> routesA = railRouteHelper.getRoutes(TrainOperatingCompanies.TP, ManchesterVictoria, Huddersfield);
        Set<Route> routesB = railRouteHelper.getRoutes(TrainOperatingCompanies.NT, Chester, Stockport);

        assertFalse(routesA.isEmpty());
        assertFalse(routesB.isEmpty());

        List<PathResults> found = new ArrayList<>();
        for(Route routeA : routesA) {
            for (Route routeB : routesB) {

                RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));
                IndexedBitSet allDates = IndexedBitSet.getIdentity(numberOfRoutes, numberOfRoutes);

                PathResults result = repository.getInterchangesFor(indexPair, allDates, interchangeStation -> true);
                if (result.hasAny()) {
                    found.add(result);
                }
            }
        }

        assertFalse(found.isEmpty());

        found.forEach(pathResults -> assertEquals(3,pathResults.getDepth()));
    }

    @Test
    void shouldReproIssueGettingInterchangesAndMissingIndexExample2() {
        IndexedBitSet allDates = IndexedBitSet.getIdentity(numberOfRoutes, numberOfRoutes);

        Set<Route> routesA = railRouteHelper.getRoutes(TrainOperatingCompanies.TP, ManchesterPiccadilly, Leeds);
        Set<Route> routesB = railRouteHelper.getRoutes(TrainOperatingCompanies.NT, Chester, ManchesterPiccadilly);

        assertFalse(routesA.isEmpty());
        assertFalse(routesB.isEmpty());

        Set<PathResults> found = new HashSet<>();
        for(Route routeA : routesA) {
            for(Route routeB : routesB) {
                RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));
                PathResults results = repository.getInterchangesFor(indexPair, allDates, interchangeStation -> true);
                if (results.hasAny()) {
                    found.add(results);
                }
            }
        }
        assertFalse(found.isEmpty());

        found.forEach(result -> {
            assertTrue(result.hasAny());
        });

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
