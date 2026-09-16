package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteDateAndDayOverlap;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.MultiMode;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
public class RouteDateAndDayOverlapTest {
    private static ComponentContainer componentContainer;

    private RouteDateAndDayOverlap overlap;
    private TramRouteHelper routeHelper;
    private TramDate date;
    private RouteCostMatrix routeMatrix;
    private RouteIndex routeIndex;
    private RouteRepository routeRepository;
    private TramchesterConfig config;

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
        routeHelper = new TramRouteHelper(componentContainer);
        routeMatrix = componentContainer.get(RouteCostMatrix.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        this.config = componentContainer.get(TramchesterConfig.class);

        overlap = componentContainer.get(RouteDateAndDayOverlap.class);

        date = TestEnv.testDay();
    }

    @Test
    void shouldTestRouteThatOverlapHaveCorrectDates() {

        UpcomingDates.daysAhead().forEach(date -> {
            Route firstRoute = routeHelper.getOneRoute(TFGMRouteNames.Blue, date);
            assertOverlaps(date, firstRoute);
        });

    }

    private void assertOverlaps(TramDate date, Route firstRoute) {
        short index = routeIndex.indexFor(firstRoute.getId());

        RouteDateAndDayOverlap.RouteOverlaps found = overlap.overlapsFor(index);
        Set<Route> allRoutes = routeRepository.getRoutes();

        Set<Route> foundOverlap = allRoutes.stream().
                filter(route -> found.get(routeIndex.indexFor(route.getId())))
                .collect(Collectors.toSet());

        assertFalse(foundOverlap.isEmpty());

        String diag = "For route " + firstRoute.getId() + " ";

        // Can only check overlaps, not that available on the specific day - might not be, but the routes might still have a
        // day when they do overlap

//        Set<Route> availableOnDay = foundOverlap.stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
//        Set<Route> mismatchOnDay = SetUtils.disjunction(foundOverlap, availableOnDay);
//        assertTrue(mismatchOnDay.isEmpty(), diag +"Got mismatch for availableOn " + date + " of " + HasId.asIds(mismatchOnDay));

        Set<Route> overlapWithRoute = foundOverlap.stream().filter(route -> route.isDateOverlap(firstRoute)).collect(Collectors.toSet());
        Set<Route> mismatchOnOverlap = SetUtils.disjunction(foundOverlap, overlapWithRoute);
        assertTrue(mismatchOnOverlap.isEmpty(), diag +"Got mismatch for overlaps " + date + " of " + HasId.asIds(mismatchOnOverlap));
    }

}
