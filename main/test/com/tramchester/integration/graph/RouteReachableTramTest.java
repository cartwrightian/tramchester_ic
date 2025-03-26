package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.*;

class RouteReachableTramTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        reachable = componentContainer.get(RouteReachable.class);
    }

    @Test
    void shouldTestGetRoutesFromStartToNeighbour() {

        TramDate when = TestEnv.testDay();
        Station start = stationRepository.getStationById(Altrincham.getId());
        Station next = stationRepository.getStationById(NavigationRoad.getId());

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8,30), Duration.ofMinutes(30), Duration.ofMinutes(30));
        List<Route> results = reachable.getRoutesFromStartToNeighbour(StationPair.of(start, next), when, timeRange, TramsOnly);

        IdSet<Route> routeIds = results.stream().collect(IdSet.collector());

        // 2->1 during March/April closures 2025
        assertEquals(1, routeIds.size(), routeIds.toString());

        assertFalse(routeIds.contains(KnownTramRoute.getEtihadPiccadillyAltrincham(when).getId()), routeIds.toString());
        assertTrue(routeIds.contains(KnownTramRoute.getBuryManchesterAltrincham(when).getId()), routeIds.toString());
    }


}
