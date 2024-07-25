package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.reference.TramStations.Ashton;
import static com.tramchester.testSupport.reference.TramStations.ShawAndCrompton;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
class RouteCalculatorKeyRoutesTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private TramDate when;
    private RouteCalculationCombinations<Station> combinations;
    private JourneyRequest journeyRequest;
    private Duration maxJourneyDuration;
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        modes = TramsOnly;
        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        int maxChanges = 4;
        journeyRequest = new JourneyRequest(when, TramTime.of(8, 5), false, maxChanges,
                maxJourneyDuration, 1, modes);
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkStationOpen(componentContainer) );
    }

    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        LocationIdPairSet<Station> stationIdPairs = combinations.EndOfRoutesToInterchanges(Tram);
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        LocationIdPairSet<Station> stationIdPairs = combinations.EndOfRoutesToEndOfRoutes(Tram);
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        LocationIdPairSet<Station> stationIdPairs = combinations.InterchangeToEndRoutes(Tram);
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        LocationIdPairSet<Station> stationIdPairs = combinations.InterchangeToInterchange(Tram);
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Disabled("WIP on why this is failing with timeout")
    @DataExpiryCategory
    @Test
    void shouldFindEndOfLinesToEndOfLinesNextNDays() {

        final LocationIdPairSet<Station> pairs = combinations.EndOfRoutesToEndOfRoutes(Tram);

        final Map<TramDate, LocationIdPairSet<Station>> missing = new HashMap<>();

        for(int day = 0; day< TestEnv.DAYS_AHEAD; day++) {
            TramDate testDate = avoidChristmasDate(when.plusDays(day));
            if (shouldCheckDate(testDate)) {
                JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8, 5), false, 3,
                        maxJourneyDuration, 1, modes);
                RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(pairs, request, Duration.ofMinutes(1));
                LocationIdPairSet<Station> missingForDate = results.getMissing();
                if (!missingForDate.isEmpty()) {
                    missing.put(testDate, missingForDate);
                }
            }
        }

        assertTrue(missing.isEmpty(), missing.toString());

    }

    private boolean shouldCheckDate(TramDate testDate) {
        return testDate.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    @DataExpiryCategory
    @Test
    void shouldFindEndOfLinesToEndOfLinesInNDays() {
        final LocationIdPairSet<Station> pairs = combinations.EndOfRoutesToEndOfRoutes(Tram);
        // helps with diagnosis when trams not running on a specific day vs. actual missing data

        TramDate testDate = avoidChristmasDate(when);
        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, modes);
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(pairs, request, Duration.ofSeconds(30));
        validateFor(results);
    }

    @Test
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {

        JourneyRequest longestJourneyRequest = new JourneyRequest(when, TramTime.of(9, 0), false, 2,
                maxJourneyDuration.multipliedBy(2), 3, modes);

        RouteCalculationCombinations.CombinationResults<Station> results =
                combinations.getJourneysFor(combinations.EndOfRoutesToEndOfRoutes(Tram), longestJourneyRequest);

        validateFor(results);

        final Optional<Duration> max = results.getValidJourneys().stream().
                map(RouteCalculatorTest::costOfJourney).
                max(Duration::compareTo);

        assertTrue(max.isPresent());
        Duration longest = max.get();

        assertTrue(Durations.greaterOrEquals(maxJourneyDuration, longest), "longest was " + longest + " more than config: " + maxJourneyDuration);
    }

    @Disabled("used for diagnosing specific issue")
    @Test
    void shouldRepoServiceTimeIssueForConcurrency() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        List<StationIdPair> stationIdPairs = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            stationIdPairs.add(StationIdPair.of(ShawAndCrompton, Ashton));
        }

        TramDate queryDate = when;
        TramTime queryTime = TramTime.of(8,0);

        Optional<Pair<StationIdPair, RouteCalculationCombinations.JourneyOrNot<Station>>> failed = stationIdPairs.parallelStream().
                map(requested -> {
                    try (MutableGraphTransaction txn = database.beginTxMutable()) {
                        JourneyRequest journeyRequest = new JourneyRequest(queryDate, queryTime, false,
                                3, maxJourneyDuration, 1, modes);
                        Optional<Journey> optionalJourney = combinations.findJourneys(txn, requested.getBeginId(), requested.getEndId(),
                                journeyRequest, () -> true);
                        RouteCalculationCombinations.JourneyOrNot<Station> journeyOrNot =
                                new RouteCalculationCombinations.JourneyOrNot<>(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

    private void validateFor(RouteCalculationCombinations.CombinationResults<Station> results) {
        LocationIdPairSet<Station> missingForDate = results.getMissing();
        assertTrue(missingForDate.isEmpty(), missingForDate.toString());
    }

}
