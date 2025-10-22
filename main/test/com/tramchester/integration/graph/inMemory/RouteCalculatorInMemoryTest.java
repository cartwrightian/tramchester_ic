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

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("WIP")
public class RouteCalculatorInMemoryTest {
    private ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;
    private GraphTransaction txn;
    private RouteCalculatorTestFacade calculator;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig(GraphDBType.InMemory);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        txn = graphDatabase.beginTx();

        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
        componentContainer.close();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
    }

    @Test
    void shouldHaveConsistentPathsDirect() {
        testForConsistency(NavigationRoad, TraffordBar);
    }

    @RepeatedTest(5)
    void shouldHaveJourney() {
        TramDate when = TestEnv.testDay();
        TramTime time = TramTime.of(17,45);
        EnumSet<TransportMode> requestedModes = EnumSet.of(TransportMode.Tram);
        Duration maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        long maxNumResults = 3;

        final JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxJourneyDuration,
                maxNumResults, requestedModes);

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

    private static @NotNull JourneyRequest getJourneyRequest() {
        TramDate when = TestEnv.testDay();
        TramTime time = TramTime.of(17,45);
        EnumSet<TransportMode> requestedModes = EnumSet.of(TransportMode.Tram);
        Duration maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        long maxNumResults = 3;

        return new JourneyRequest(when, time, false, 1, maxJourneyDuration,
                maxNumResults, requestedModes);
    }
}
