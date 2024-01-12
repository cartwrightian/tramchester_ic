package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.BusStations.CentralStops;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.util.EnumSet;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;


@BusTest
public class BusRouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private StationRepository stationRepository;
    private TramDate date;
    private TimeRange timeRange;
    private EnumSet<TransportMode> modes;
    private CentralStops centralStops;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        stationRepository = componentContainer.get(StationRepository.class);

        centralStops = new CentralStops(componentContainer);

        date = TestEnv.testDay();
        timeRange = TimeRange.of(TramTime.of(4,45), TramTime.of(23,55));
        modes = BusesOnly;
;    }

    // For testing, likely to vary a lot with timetable updates
    @Disabled("Changes too often to be useful")
    @Test
    void shouldHaveExpectedNumber() {
        assertEquals(1827904, routeToRouteCosts.size());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamStockport() {
        StationGroup start = centralStops.Altrincham();
        StationGroup end = centralStops.Stockport();

        // one for the temp stockport bus station, was zero, seems direct alty buses terminating somewhere else
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(start, end, date, timeRange, modes).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamShudehill() {
        StationGroup start = centralStops.Altrincham();
        StationGroup end = centralStops.Shudehill(); //stationGroupsRepository.findByName("Shudehill Interchange");

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(start, end, date, timeRange, modes);
        assertEquals(1, numberOfChanges.getMin());
        assertEquals(3, numberOfChanges.getMax());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenKnutsfordAndShudehill() {
        Station start = stationRepository.getStationById(BusStations.KnutsfordStationStand3.getId());
        StationGroup end = centralStops.Shudehill(); // stationGroupsRepository.findByName("Shudehill Interchange");

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(LocationSet.singleton(start),
                LocationSet.of(end.getContained()), date, timeRange, modes);

        assertEquals(2, numberOfChanges.getMin());
        assertEquals(3, numberOfChanges.getMax());
    }

//    @Test
//    void shouldHaveCorrectCostBetweenRoutesDiffDirections() {
//
//        IdFor<Agency> agencyId = Agency.createId("DAGC");
//        Set<Route> altyToKnutsford = routeRepository.findRoutesByName(agencyId, KnownBusRoute.AltrinchamMacclesfield.getName());
//        assertEquals(2, altyToKnutsford.size());
//        Set<Route> knutsfordToAlty = routeRepository.findRoutesByName(agencyId, "Macclesfield - Altrincham");
//        assertEquals(2, knutsfordToAlty.size());
//
//        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(altyToKnutsford.iterator().next(),
//                knutsfordToAlty.iterator().next(), date, timeRange, modes);
//        assertEquals(1, numberOfChanges.getMin());
//
//    }

}
