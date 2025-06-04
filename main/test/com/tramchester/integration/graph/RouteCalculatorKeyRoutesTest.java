package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.integration.testSupport.LocationIdsAndNames;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.DataExpiryTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
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
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).endOfRoutesToInterchanges(Tram).stream().
                filter(pair -> !UpcomingDates.hasClosure(pair, when)).
                collect(LocationIdPairSet.collector());
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindEndOfRoutesToEndOfRoute() {
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Tram);
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindInterchangesToEndOfRoutes() {
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).interchangeToEndRoutes(Tram)
                .stream().filter(pair -> !UpcomingDates.hasClosure(pair, when)).
                collect(LocationIdPairSet.collector());
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @Test
    void shouldFindInterchangesToInterchanges() {
        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).interchangeToInterchange(Tram).stream().
                filter(pair -> !UpcomingDates.hasClosure(pair, when)).
                collect(LocationIdPairSet.collector());
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
        validateFor(results);
    }

    @DataExpiryTest
    @Test
    void shouldFindEndOfLinesToEndOfLinesNextNDays() {

        final LocationIdPairSet<Station> pairs = combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Tram);

        final Map<TramDate, LocationIdsAndNames<Station>> missing = new HashMap<>();

        UpcomingDates.daysAhead().stream().filter(this::shouldCheckDate).forEach(testDate -> {
            JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8, 5), false, 3,
                    maxJourneyDuration, 1, modes);
            Running running = () -> true;
            RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(pairs, request, Duration.ofMinutes(1),
                    running);
            LocationIdsAndNames<Station> missingForDate = results.getMissing();
            if (!missingForDate.isEmpty()) {
                missing.put(testDate, missingForDate);
            }
        });

        assertTrue(missing.isEmpty(), missing.toString());

    }

    private boolean shouldCheckDate(TramDate testDate) {
        if (UpcomingDates.validTestDate(testDate)) {
            return false;
        }
        return testDate.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    @DataExpiryTest
    @Test
    void shouldFindEndOfLinesToEndOfLinesInNDays() {
        final LocationIdPairSet<Station> pairs = combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Tram);
        // helps with diagnosis when trams not running on a specific day vs. actual missing data

        TramDate testDate = UpcomingDates.avoidChristmasDate(when);
        JourneyRequest request = new JourneyRequest(testDate, TramTime.of(8,5), false, 4,
                maxJourneyDuration, 1, modes);
        Running running = () -> true;
        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(pairs, request,
                Duration.ofSeconds(30), running);
        validateFor(results);
    }

    @Test
    void shouldFindEndOfLinesToEndOfLinesFindLongestDuration() {

        JourneyRequest longestJourneyRequest = new JourneyRequest(when, TramTime.of(9, 0), false, 2,
                maxJourneyDuration.multipliedBy(2), 3, modes);

        RouteCalculationCombinations.CombinationResults<Station> results =
                combinations.getJourneysFor(combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Tram), longestJourneyRequest);

        validateFor(results);

        final Optional<Duration> max = results.getValidJourneys().stream().
                map(RouteCalculatorTest::costOfJourney).
                max(Duration::compareTo);

        assertTrue(max.isPresent());
        Duration longest = max.get();

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
                    try (ImmutableGraphTransaction txn = database.beginTx()) {
                        JourneyRequest journeyRequest = new JourneyRequest(queryDate, queryTime, false,
                                3, maxJourneyDuration, 1, modes);
                        final Optional<Journey> optionalJourney = combinations.findJourneys(txn, stationIdPair.getBeginId(), stationIdPair.getEndId(),
                                journeyRequest, () -> true);
                        RouteCalculationCombinations.JourneyOrNot<Station> journeyOrNot =
                                combinations.createResult(stationIdPair, queryDate, queryTime, optionalJourney);
                        return Pair.of(stationIdPair, journeyOrNot);
                    }
                }).filter(pair -> pair.getRight().missing()).findAny();

        assertFalse(failed.isPresent());
    }

    private void validateFor(RouteCalculationCombinations.CombinationResults<Station> results) {
        LocationIdsAndNames<Station> missingForDate = results.getMissing();
        assertTrue(missingForDate.isEmpty(), missingForDate.toString());
    }

}
