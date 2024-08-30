package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.SimpleImmutableBitmap;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
public class RouteCostMatrixTest {
    private static ComponentContainer componentContainer;

    private TramRouteHelper routeHelper;
    private TramDate date;
    private RouteCostMatrix routeMatrix;
    private RouteIndex routeIndex;
    private RouteRepository routeRepository;

    // NOTE: this test does not cause a full db rebuild, so might see VERSION node missing messages

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {

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
        routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
        routeMatrix = componentContainer.get(RouteCostMatrix.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        date = TestEnv.testDay();
    }

    @Test
    void shouldHaveExpectedIndexWhereDirectInterchangePossible() {
        Route routeA = routeHelper.getOneRoute(BuryManchesterAltrincham, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport_OLD, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldHaveExpectedIndexWhereNoDirectInterchangePossible() {
        Route routeA = routeHelper.getOneRoute(PiccadillyBury_OLD, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldHaveExpectedIndexForEcclesRouteOntoAltyRoute() {
        Route routeA = routeHelper.getOneRoute(EcclesManchesterAshtonUnderLyne_OLD, date);
        Route routeB = routeHelper.getOneRoute(BuryManchesterAltrincham, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldHaveExpectedIndexForEcclesRouteFromAltyRoute() {
        Route routeA = routeHelper.getOneRoute(BuryManchesterAltrincham, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport_OLD, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldGetBitsSetIfAlreadySetForLowerDepth() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(BuryManchesterAltrincham, date);

        RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));

        int firstDepth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        // since changes now the same depth
        assertEquals(1, firstDepth);

        // set for depth 1, so ought to be set for all subsequent depths

        SimpleImmutableBitmap rowAtDepthOne = routeMatrix.getExistingBitSetsForRoute(indexPair.first(), 1);
        assertTrue(rowAtDepthOne.get(indexPair.second()));

        SimpleImmutableBitmap rowAtDepthTwo = routeMatrix.getExistingBitSetsForRoute(indexPair.first(), 2);
        assertTrue(rowAtDepthTwo.get(indexPair.second()));

        SimpleImmutableBitmap rowAtDepthThree = routeMatrix.getExistingBitSetsForRoute(indexPair.first(), 3);
        assertTrue(rowAtDepthThree.get(indexPair.second()));

    }

    @Test
    void shouldHaveUniqueDegreeForEachRoutePair() {
        Set<Route> onDate = routeRepository.getRoutesRunningOn(date);

        assertFalse(onDate.isEmpty());

        onDate.forEach(first -> onDate.forEach(second -> {
            RoutePair routePair = RoutePair.of(first, second);

            if (!routePair.areSame()) {
                RouteIndexPair indexPair = routeIndex.getPairFor(routePair);
                List<Integer> results = routeMatrix.getAllDegrees(indexPair);
                assertTrue(results.size()<=1, "Too many degrees " + results + " for " +
                        indexPair + " " +routePair + " on " + date);
            }
        }));
    }


}
