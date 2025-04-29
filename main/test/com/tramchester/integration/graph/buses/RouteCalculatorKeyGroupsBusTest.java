package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

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
    private RouteCalculationCombinations<StationLocalityGroup> combinations;
    private JourneyRequest journeyRequest;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().
                create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
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
        LocationIdPairSet<StationLocalityGroup> pairs = createPairsFor(Arrays.asList(Altrincham, Stockport, ManchesterAirport));
        combinations.validateAllHaveAtLeastOneJourney(pairs, journeyRequest, true);
    }

    @Test
    void shouldCheckResultsProcessing() {
        LocationIdPairSet<StationLocalityGroup> pairs = createPairsFor(Arrays.asList(Altrincham, Stockport, ManchesterAirport));
        RouteCalculationCombinations.CombinationResults<StationLocalityGroup> results = combinations.getJourneysFor(pairs, journeyRequest);

        List<RouteCalculationCombinations.JourneyOrNot<StationLocalityGroup>> failed = results.getFailed();

        assertTrue(failed.isEmpty());
    }

    @Disabled("takes too long for this many of stations")
    @Test
    void shouldCheckForAllKnownGMLocalities() {
        TramTime time = TramTime.of(11, 30);
        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

        LocationIdPairSet<StationLocalityGroup> pairs = createPairsFor(new ArrayList<>(GreaterManchester));
        combinations.validateAllHaveAtLeastOneJourney(pairs, request, true);
    }

    private LocationIdPairSet<StationLocalityGroup> createPairsFor(List<KnownLocality> localities) {
        LocationIdPairSet<StationLocalityGroup> pairs = new LocationIdPairSet<>();
        for(final KnownLocality placeA : localities) {
            for(final KnownLocality placeB : localities) {
                if (placeA!=placeB) {
                    LocationIdPair<StationLocalityGroup> pair = new LocationIdPair<>(placeA.getId(), placeB.getId());
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }

}
