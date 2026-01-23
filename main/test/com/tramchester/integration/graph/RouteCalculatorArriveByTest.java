package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.Cornbrook;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
public class RouteCalculatorArriveByTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    public static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private int maxChanges;

    private static EnumSet<TransportMode> requestedModes;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();
    private GraphTransaction txn;
    private TramDuration maxJourneyDuration;
    private int maxNumResults;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
        requestedModes = TramsOnly;
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        maxJourneyDuration = TramDuration.ofMinutes(config.getMaxJourneyDuration());
        maxNumResults = config.getMaxNumberResults();
        maxChanges = config.getMaxNumberChanges();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveExpectedPathsForSimpleJourney() {
        TramTime arriveByTime = TramTime.of(8, 15);

        JourneyRequest journeyRequest = new JourneyRequest(when, arriveByTime, true,
                maxChanges, maxJourneyDuration, maxNumResults, requestedModes);

        List<Journey> results = calculator.calculateRouteAsList(Altrincham, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty());

        Set<Journey> arriveBefore = results.stream().filter(journey -> journey.getArrivalTime().isBefore(arriveByTime)).collect(Collectors.toSet());

        assertFalse(arriveBefore.isEmpty(), "no results before " + arriveByTime + " in " + results);

    }

    @Test
    void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime arriveByTime = TramTime.of(11,45);

        JourneyRequest journeyRequest = new JourneyRequest(when, arriveByTime, true,
                maxChanges, maxJourneyDuration, maxNumResults, requestedModes);

        List<Journey> results = calculator.calculateRouteAsList(Altrincham, Cornbrook, journeyRequest);

        Set<Journey> arriveBefore = results.stream().filter(journey -> journey.getArrivalTime().isBefore(arriveByTime)).collect(Collectors.toSet());
        assertFalse(arriveBefore.isEmpty(), "no results before " + arriveByTime + " in " + results);

        List<Journey> found = new ArrayList<>();
        arriveBefore.forEach(journey -> {
            TramTime firstDepartureTime = journey.getArrivalTime();
            assertTrue(firstDepartureTime.isBefore(arriveByTime), firstDepartureTime + " not before " + arriveByTime);
            // TODO lockdown less frequent services during lockdown mean threshhold here increased to 12
            Duration duration = Duration.between(journey.getArrivalTime().asLocalTime(), arriveByTime.asLocalTime());
            if (duration.getSeconds()<=(12*60)) {
                found.add(journey);
            }
        });
        Assertions.assertFalse(found.isEmpty(), "no journeys found");
    }


}