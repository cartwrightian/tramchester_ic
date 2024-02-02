package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.LocationIdPairSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@BusTest
@Disabled("takes too long for this many of stations")
class RouteCalculatorKeyRoutesBusTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private final TramDate when = TestEnv.testDay();
    private RouteCalculationCombinations<Station> combinations;
    private JourneyRequest journeyRequest;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        testConfig = new SubGraphConfig();
        TestEnv.deleteDBIfPresent(testConfig);
        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorKeyRoutesBusTest::configureFilter).
                create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        graphFilter.addAgency(TestEnv.WarringtonsOwnBuses.getId());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(testConfig);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkStationOpen(componentContainer) );
        TramTime time = TramTime.of(8, 0);
        int numberChanges = 3;
        journeyRequest = new JourneyRequest(when, time, false, numberChanges,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);
    }

    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        LocationIdPairSet<Station> stationIdPairs = combinations.EndOfRoutesToInterchanges(Bus);
        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true);
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        LocationIdPairSet<Station> stationIdPairs = combinations.EndOfRoutesToEndOfRoutes(Bus);
        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true);
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        LocationIdPairSet<Station> stationIdPairs = combinations.InterchangeToEndRoutes(Bus);
        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true);
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        LocationIdPairSet<Station> stationIdPairs = combinations.InterchangeToInterchange(Bus);
        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true);
    }

    private static class SubGraphConfig extends IntegrationBusTestConfig {
        @Override
        public boolean isGraphFiltered() {
            return true;
        }
    }
}
