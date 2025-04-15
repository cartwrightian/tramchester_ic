package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.closures.StationClosuresListForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteCalculatorCloseStationsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private RouteCalculatorTestFacade calculator;
    private final static TramDate when = TestEnv.testDay();
    private ImmutableGraphTransaction txn;

    private final static TramDate begin = when.plusWeeks(1);
    private final static TramDate end = when.plusWeeks(2);

    // see note below on DB deletion
    private final static List<StationClosures> closedStations = Arrays.asList(
            new StationClosuresListForTest(Shudehill, new DateRange(begin, end), true),
            new StationClosuresListForTest(PiccadillyGardens, new DateRange(begin, end), false));

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramClosedStationsTestConfig(closedStations, true,
                Collections.emptyList());

        // if above closedStation list is changed need to enable this once
        //TestEnv.deleteDBIfPresent(config);

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
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldFindUnaffectedRouteNormally() {
        JourneyRequest journeyRequest = new JourneyRequest(begin, TramTime.of(8,0), false,
                2, Duration.ofMinutes(120), 1, getRequestedModes());
        List<Journey> result = calculator.calculateRouteAsList(TramStations.Altrincham, TraffordBar, journeyRequest);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldHandlePartialClosure() {
        // appears to be an issue with data more than 1 week out with missing routes for ExchangeSquare and other stations...
        JourneyRequest journeyRequest = new JourneyRequest(begin.plusDays(1), TramTime.of(8,0), false,
                3, Duration.ofMinutes(120), 1, getRequestedModes());
        List<Journey> result = calculator.calculateRouteAsList(ExchangeSquare, StPetersSquare, journeyRequest);
        assertFalse(result.isEmpty(), "no journey for " + journeyRequest);
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return TramsOnly;
    }

    @Test
    void shouldNotFindRouteToClosedStationViaDirectTram() {
        Set<Journey> singleStage = getSingleStageBuryToEccles(begin);
        assertTrue(singleStage.isEmpty());
    }

    @Test
    void shouldFindRouteToClosedStationViaDirectTramWhenAfterClosurePeriod() {
        TramDate travelDate = UpcomingDates.avoidChristmasDate(end.plusDays(2));

        Set<Journey> singleStage = getSingleStageBuryToEccles(travelDate);
        assertFalse(singleStage.isEmpty());
    }

    @NotNull
    private Set<Journey> getSingleStageBuryToEccles(TramDate travelDate) {
        JourneyRequest journeyRequest = new JourneyRequest(travelDate, TramTime.of(8, 0),
                false, 0, Duration.ofMinutes(120), 1, getRequestedModes());

        List<Journey> journeys = calculator.calculateRouteAsList(Bury, Shudehill, journeyRequest);
        return journeys.stream().filter(results -> results.getStages().size() == 1).collect(Collectors.toSet());
    }

}
