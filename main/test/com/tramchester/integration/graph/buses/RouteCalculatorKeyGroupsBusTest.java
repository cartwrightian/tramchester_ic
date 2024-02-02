package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.LocationIdPairSet;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static com.tramchester.testSupport.reference.KnownLocality.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@BusTest
class RouteCalculatorKeyGroupsBusTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private final TramDate when = TestEnv.testDay();
    private RouteCalculationCombinations<StationGroup> combinations;
    private JourneyRequest journeyRequest;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().
                create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkGroupOpen(componentContainer) );
        TramTime time = TramTime.of(10, 30);
        int numberChanges = 3;
        journeyRequest = new JourneyRequest(when, time, false, numberChanges,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);
    }

    @Test
    void shouldCheckSomeKnownLocalities() {
        LocationIdPairSet<StationGroup> pairs = createPairsFor(Arrays.asList(Altrincham, Stockport, ManchesterAirport));
        combinations.validateAllHaveAtLeastOneJourney(pairs, journeyRequest, true);
    }

    @Test
    void shouldCheckResultsProcessing() {
        LocationIdPairSet<StationGroup> pairs = createPairsFor(Arrays.asList(Altrincham, Stockport, ManchesterAirport));
        RouteCalculationCombinations.CombinationResults<StationGroup> results = combinations.getJourneysFor(pairs, journeyRequest);

        List<RouteCalculationCombinations.JourneyOrNot<StationGroup>> failed = results.getFailed();

        assertTrue(failed.isEmpty());
    }

    @Disabled("takes too long for this many of stations")
    @Test
    void shouldCheckForAllKnownGMLocalities() {
        TramTime time = TramTime.of(11, 30);
        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

        LocationIdPairSet<StationGroup> pairs = createPairsFor(new ArrayList<>(GreaterManchester));
        combinations.validateAllHaveAtLeastOneJourney(pairs, request, true);
    }

    private LocationIdPairSet<StationGroup> createPairsFor(List<KnownLocality> localities) {
        LocationIdPairSet<StationGroup> pairs = new LocationIdPairSet<>();
        for(final KnownLocality placeA : localities) {
            for(final KnownLocality placeB : localities) {
                if (placeA!=placeB) {
                    LocationIdPair<StationGroup> pair = new LocationIdPair<>(placeA.getId(), placeB.getId(), LocationType.StationGroup);
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }

}
