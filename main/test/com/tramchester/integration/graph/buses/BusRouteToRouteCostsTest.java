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
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.util.EnumSet;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static com.tramchester.testSupport.reference.KnownLocality.Shudehill;
import static org.junit.jupiter.api.Assertions.assertEquals;


@BusTest
public class BusRouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private StationRepository stationRepository;
    private StationGroupsRepository stationGroupsRepository;
    private TramDate date;
    private TimeRange timeRange;
    private EnumSet<TransportMode> modes;

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
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

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
        StationGroup start = KnownLocality.Altrincham.from(stationGroupsRepository);
        StationGroup end = KnownLocality.Stockport.from(stationGroupsRepository);

        // one for the temp stockport bus station, was zero, seems direct alty buses terminating somewhere else
        assertEquals(0, routeToRouteCosts.getNumberOfChanges(start, end, date, timeRange, modes).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamShudehill() {
        StationGroup start = KnownLocality.Altrincham.from(stationGroupsRepository);;
        StationGroup end = Shudehill.from(stationGroupsRepository);

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(start, end, date, timeRange, modes);
        assertEquals(1, numberOfChanges.getMin());
        assertEquals(3, numberOfChanges.getMax());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenKnutsfordAndShudehill() {
        Station start = stationRepository.getStationById(BusStations.KnutsfordStationStand3.getId());
        StationGroup end = Shudehill.from(stationGroupsRepository);

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(LocationSet.singleton(start),
                LocationSet.of(end.getContained()), date, timeRange, modes);

        assertEquals(2, numberOfChanges.getMin());
        assertEquals(3, numberOfChanges.getMax());
    }

}
