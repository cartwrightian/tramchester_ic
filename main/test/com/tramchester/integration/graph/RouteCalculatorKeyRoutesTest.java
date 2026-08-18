package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.integration.testSupport.LocationIdsAndNames;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import com.tramchester.testSupport.testTags.DataExpiryTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Tram;
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
    private TramDuration maxJourneyDuration;
    private ImmutableEnumSet<TransportMode> modes;
    private int maxChanges;

    /// NOTES
    /// Often fix is to add an additional interchange i.e. Needing MediaCityUK as an interchange
    ///

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
        modes = TransportMode.TramsOnly;
        maxJourneyDuration = TramDuration.ofMinutes(testConfig.getMaxJourneyDuration());
        maxChanges = testConfig.getMaxNumberChanges();
        journeyRequest = new JourneyRequest(when, TramTime.of(8, 5), false, maxChanges,
                maxJourneyDuration, 1, modes);
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkStationOpen(componentContainer) );
    }

    @DisabledUntilDate(year = 2026, month = 8, day = 20)
    @Test
    void shouldFindEndOfRoutesToInterchanges() {
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).endOfRoutesToInterchanges(Tram).stream().
                filter(pair -> !UpcomingDates.hasClosure(pair, when)).
                collect(LocationIdPairSet.collector());
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @DisabledUntilDate(year = 2026, month = 8, day = 20)

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Tram);
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @DisabledUntilDate(year = 2026, month = 8, day = 20)
    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).interchangeToEndRoutes(Tram)
                .stream().filter(pair -> !UpcomingDates.hasClosure(pair, when)).
                collect(LocationIdPairSet.collector());
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @DisabledUntilDate(year = 2026, month = 8, day = 20)
    @Test
    void shouldFindInterchangesToInterchanges() {
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).interchangeToInterchange(Tram).stream().
                filter(pair -> !UpcomingDates.hasClosure(pair, when)).
                collect(LocationIdPairSet.collector());
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @DisabledUntilDate(year = 2026, month = 8, day = 20)
    @DataExpiryTest
    @Test
    void shouldFindEndOfLinesToEndOfLinesNextNDays() {


        final SortedMap<TramDate, LocationIdsAndNames<Station>> missing = new TreeMap<>();

        Duration timeout = Duration.ofMinutes(1);
        TramTime tramTime = TramTime.of(8, 5);

        UpcomingDates.daysAhead().stream().
                filter(UpcomingDates::notChristmasPeriod).
                forEach(testDate -> {
                    final LocationIdPairSet<Station> pairs = combinations.getCreatePairs(testDate).
                            endOfRoutesToEndOfRoutes(Tram);

                    if (!pairs.isEmpty()) {
                        final JourneyRequest request = new JourneyRequest(testDate, tramTime, false, 2,
                                maxJourneyDuration, 1, modes);
                        final Running running = () -> true;
                        final RouteCalculationCombinations.CombinationResults<Station> results =
                                combinations.getJourneysFor(pairs, request, timeout, running);
                        final LocationIdsAndNames<Station> missingForDate = results.getMissing();
                        if (!missingForDate.isEmpty()) {
                            missing.put(testDate, missingForDate);
                        }
                    }
        });

        assertTrue(missing.isEmpty(), missing.toString());

    }

    @DisabledUntilDate(year = 2026, month = 8, day = 20)
    @DataExpiryTest
    @Test
    void shouldFindEndOfLinesToEndOfLinesInNDays() {
        final LocationIdPairSet<Station> pairs = combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Tram);
        // helps with diagnosis when trams not running on a specific day vs. actual missing data

        TramDate testDate = UpcomingDates.avoidChristmasDate(when);
        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(9,5), false, maxChanges,
                maxJourneyDuration, 1, modes);
        Running running = () -> true;
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(pairs, request,
                Duration.ofSeconds(30), running);
        validateFor(results);
    }

    @Test
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {

        TramDuration twice = maxJourneyDuration.plus(maxJourneyDuration);

        JourneyRequest longestJourneyRequest = new JourneyRequest(when, TramTime.of(9, 0), false, 2,
                twice, 3, modes);

        RouteCalculationCombinations.CombinationResults<Station> results =
                combinations.getJourneysFor(combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Tram), longestJourneyRequest);

        List<Journey> found = results.getValidJourneys();
        //assertEquals(447, found.size());
        assertFalse(found.isEmpty());

        final Optional<TramDuration> max = results.getValidJourneys().stream().
                map(RouteCalculatorTest::costOfJourney).
                max(TramDuration::compareTo);

        assertTrue(max.isPresent());
        TramDuration longest = max.get();

        assertTrue(Durations.greaterOrEquals(maxJourneyDuration, longest), "longest was " + longest + " more than config: "
                + maxJourneyDuration);
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
                map(stationIdPair -> {
                    try (GraphTransaction txn = database.beginTx()) {
                        JourneyRequest journeyRequest = new JourneyRequest(queryDate, queryTime, false,
                                3, maxJourneyDuration, 1, modes);
                        final Optional<Journey> optionalJourney = combinations.findJourneys(txn, stationIdPair.getBeginLocationId(), stationIdPair.getEndLocationId(),
                                journeyRequest, () -> true);
                        RouteCalculationCombinations.JourneyOrNot<Station> journeyOrNot =
                                combinations.createResult(stationIdPair, queryDate, queryTime, optionalJourney);
                        return Pair.of(stationIdPair, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

    private void validateFor(RouteCalculationCombinations.CombinationResults<Station> results) {
        RouteCalculationCombinations.Failures<Station> missingForDate = results.getFailed();
        assertTrue(missingForDate.isEmpty(), missingForDate.toString());
    }

}
