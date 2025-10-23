package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.integration.graph.RouteCalculatorTest.TXN_TIMEOUT;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteCalculatorInMemoryTest {
    private static EnumSet<TransportMode> requestedModes;
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private static GraphDatabase database;

    private final TramDate when = TestEnv.testDay();
    private GraphTransaction txn;
    private RouteCalculatorTestFacade calculator;
    private Duration maxJourneyDuration;
    private int maxNumResults;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig(GraphDBType.InMemory);
        requestedModes = TramsOnly;
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        maxNumResults = config.getMaxNumResults();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveConsistentPathsDirect() {
        testForConsistency(NavigationRoad, TraffordBar);
    }

    @Test
    void shouldHaveJourney() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(17,45), 3, 1);

        List<Journey> journeys = calculator.calculateRouteAsList(Altrincham, Ashton, journeyRequest);

        if (journeys.isEmpty()) {
            journeyRequest.setDiag(true);
            journeys = calculator.calculateRouteAsList(Altrincham, Ashton, journeyRequest);
            assertTrue(journeys.isEmpty());
            fail("failed, diag was on");
        }
    }

    @RepeatedTest(5)
    void shouldHaveConsistentPathsChangeRequired() {
        testForConsistency(Timperley, ManAirport);
    }

    private void testForConsistency(TramStations start, TramStations end) {
        final JourneyRequest journeyRequest = getJourneyRequest();
        List<Journey> journeys = calculator.calculateRouteAsList(start, end, journeyRequest);
        assertFalse(journeys.isEmpty(), "no journeys found");

        int count = 10;

        while (count-- > 0) {
            List<Journey> again = calculator.calculateRouteAsList(start, end, journeyRequest);
            assertEquals(journeys, again);
        }
    }

    private @NotNull JourneyRequest getJourneyRequest() {
        TramDate when = TestEnv.testDay();
        TramTime time = TramTime.of(17,45);

//        return new JourneyRequest(when, time, false, 1, maxJourneyDuration,
//                maxNumResults, requestedModes);

        return standardJourneyRequest(when, time, maxNumResults, 1);
    }

    @NotNull
    private JourneyRequest standardJourneyRequest(TramDate date, TramTime time, long maxNumberJourneys, int maxNumberChanges) {
        return new JourneyRequest(date, time, false, maxNumberChanges, maxJourneyDuration, maxNumberJourneys, requestedModes);
    }
}
