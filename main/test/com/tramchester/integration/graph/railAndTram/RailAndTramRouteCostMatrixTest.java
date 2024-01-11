package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.caching.FileDataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.Route;
import com.tramchester.domain.collections.ImmutableIndexedBitSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteDateAndDayOverlap;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.RailRouteHelper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteCostMatrixTest {
    private static ComponentContainer componentContainer;

    private TramRouteHelper routeHelper;
    private TramDate date;
    private RouteCostMatrix routeMatrix;
    private StationRepository stationRepository;
    private RailRouteHelper railRouteHelper;
    private RouteIndex routeIndex;
    private int numberOfRoutes;

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
        routeHelper = new TramRouteHelper(routeRepository);
        railRouteHelper = new RailRouteHelper(componentContainer);
        routeMatrix = componentContainer.get(RouteCostMatrix.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        numberOfRoutes = routeRepository.numberOfRoutes();
        date = TestEnv.testDay();
    }

    @Disabled("did not find the issue, always passes")
    @Test
    void shouldHaveConsistentNumbersOnCostsPerDegree() {

        // For tracking down bug, seems consistent for one data load, but not across multiple?

        final ImmutableIndexedBitSet resultA = routeMatrix.getCostsPerDegree(1);

        for (int i = 0; i < 1000; i++) {
            RouteCostMatrix secondMatrix = createRouteCostMatrix();

            ImmutableIndexedBitSet resultB = secondMatrix.getCostsPerDegree(1);

            assertEquals(resultA, resultB);
        }
    }

    @Disabled("did not find the issue, always passes")
    @Test
    void shouldHaveDateOverlapsConsistently() {

        // seeing each run produce consistent results, but across multiples runs see different numbers, which is incorrect
        NumberOfRoutes hasNumberRoutes = () -> numberOfRoutes;

        assertEquals(505, hasNumberRoutes.numberOfRoutes());

        RouteDateAndDayOverlap overlapsA = new RouteDateAndDayOverlap(routeIndex, hasNumberRoutes);

        overlapsA.start();
        final int previous = overlapsA.numberBitsSet();

        for (int i = 0; i < 100000; i++) {
            RouteDateAndDayOverlap overlapsB = new RouteDateAndDayOverlap(routeIndex, hasNumberRoutes);
            overlapsB.start();
            assertEquals(previous, overlapsB.numberBitsSet());
        }

    }

    @Test
    void shouldHaveExpectedIndexWhereDirectTramInterchangePossible() {
        Route routeA = routeHelper.getOneRoute(BuryManchesterAltrincham, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldHaveExpectedIndexWhereOneChangeTrainInterchangePossible() {
        Station stockport = stationRepository.getStationById(RailStationIds.Stockport.getId());
        Station piccadilly = stationRepository.getStationById(RailStationIds.ManchesterPiccadilly.getId());

        Set<Route> stockportPickups = stockport.getPickupRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
        Set<Route> piccDropoffs = piccadilly.getDropoffRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        assertFalse(stockportPickups.isEmpty());
        assertFalse(piccDropoffs.isEmpty());

        // not all routes will overlap without 1 change, but we should have some that do
        AtomicInteger oneChange = new AtomicInteger(0);
        stockportPickups.forEach(dropOff -> piccDropoffs.forEach(pickup -> {
            if (!dropOff.equals(pickup)) {
                int depth = routeMatrix.getConnectionDepthFor(dropOff, pickup);
                if (depth==1) {
                    oneChange.incrementAndGet();
                }
            }
        }));
        assertNotEquals(0, oneChange.get());
    }

    @Test
    void shouldHaveExpectedIndexWhereNoDirectInterchangePossible() {
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(2, depth);
    }

    @Test
    void shouldHaveCorrectIndexBetweenTramAndRailRoutes() {
        Station altrinchamTram = stationRepository.getStationById(TramStations.Altrincham.getId());
        Station altrinchamRail = stationRepository.getStationById(RailStationIds.Altrincham.getId());

        Set<Route> railDropOffs = altrinchamRail.getDropoffRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
        Set<Route> tramPickups = altrinchamTram.getPickupRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        assertFalse(railDropOffs.isEmpty());
        assertFalse(tramPickups.isEmpty());

        railDropOffs.forEach(dropOff -> tramPickups.forEach(pickup -> {
            int depth = routeMatrix.getConnectionDepthFor(dropOff, pickup);
            assertEquals(1, depth, "wrong depth between " + dropOff.getId() + " and " + pickup.getId());
        }));

    }

    @Test
    void shouldRHaveChangesBetweenLiverpoolAndCreweRoutes() {
        // repro issue in routecostmatric

        Route routeA = railRouteHelper.getRoute(TrainOperatingCompanies.NT, ManchesterVictoria, LiverpoolLimeStreet, 1);
        Route routeB = railRouteHelper.getRoute(TrainOperatingCompanies.NT, Crewe, ManchesterPiccadilly, 2);

        int result = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(2, result);
    }

    @NotNull
    private RouteCostMatrix createRouteCostMatrix() {
        NumberOfRoutes numberOfRoutes = componentContainer.get(NumberOfRoutes.class);
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        FileDataCache dataCache = componentContainer.get(FileDataCache.class);
        GraphFilterActive graphFilter = componentContainer.get(GraphFilterActive.class);
        RouteDateAndDayOverlap routeDayAndDateOverlap = componentContainer.get(RouteDateAndDayOverlap.class);

        RouteCostMatrix secondMatrix = new RouteCostMatrix(numberOfRoutes, interchangeRepository, dataCache, graphFilter,
                routeIndex, routeDayAndDateOverlap);

        secondMatrix.start();
        return secondMatrix;
    }

}
