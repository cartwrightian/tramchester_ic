package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class BusRouteCalculatorTest {

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;

    private RouteCalculatorTestFacade calculator;
    private StationGroupsRepository stationGroupsRepository;
    private StationRepository stationRepository;

    private final TramDate when = TestEnv.testDay();
    private MutableGraphTransaction txn;
    private Duration maxJourneyDuration;
    private StationGroup stockportBusStation;
    private StationGroup altrinchamInterchange;
    private StationGroup knutsfordBusStation;
    private StationGroup shudehillInterchange;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        stockportBusStation = stationGroupsRepository.findByName(Composites.StockportTempBusStation.getName());
        altrinchamInterchange = stationGroupsRepository.findByName(Composites.AltrinchamInterchange.getName());

        knutsfordBusStation = stationGroupsRepository.getStationGroup(KnutfordStationAreaId);
        shudehillInterchange = stationGroupsRepository.findByName("Shudehill Interchange");
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveValidStations() {
        assertNotNull(stockportBusStation);
        assertNotNull(altrinchamInterchange);
        assertNotNull(knutsfordBusStation);
        assertNotNull(shudehillInterchange);
    }

    @Test
    void shouldHaveStockToAltyJourneyAndBackAgainOneChanges() {

        TramTime travelTime = TramTime.of(9,0);
        TramDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> journeysA = calculator.calculateRouteAsSet(stockportBusStation, altrinchamInterchange, requestA);
        assertFalse(journeysA.isEmpty());

        JourneyRequest requestB = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> journeysB = calculator.calculateRouteAsSet(altrinchamInterchange, stockportBusStation, requestB);
        assertFalse(journeysB.isEmpty());
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return BusesOnly;
    }

    @Test
    void shouldHaveStockToAltyJourney() {

        TramTime travelTime = TramTime.of(9, 0);
        TramDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 3, getRequestedModes());
        Set<Journey> journeys = calculator.calculateRouteAsSet(stockportBusStation, altrinchamInterchange, requestA);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAltyToStockport() {

        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextMonday(),
                TramTime.of(9, 0), false, 1, maxJourneyDuration, 3, getRequestedModes());

        Set<Journey> journeysMaxChanges = calculator.calculateRouteAsSet(altrinchamInterchange, stockportBusStation, journeyRequest);

        // algo seems to return very large number of changes even when 2 is possible??
        List<Journey> journeys2Stages = journeysMaxChanges.stream().
                filter(journey -> countNonConnectStages(journey) <= 3).
                toList();
        assertFalse(journeys2Stages.isEmpty());
    }

    private long countNonConnectStages(Journey journey) {
        return journey.getStages().stream().
                filter(stage -> stage.getMode().getTransportMode() != TransportMode.Connect).count();
    }

    @Test
    void shouldFindJourneyInFutureCorrectly() {
        // attempt to repro seen in ui where zero journeys
        StationGroup start = stationGroupsRepository.findByName("Taylor Road, Oldfield Brow, Altrincham");

        assertNotNull(start);

        final TramDate futureDate = TestEnv.testDay().plusDays(14);
        JourneyRequest journeyRequest = new JourneyRequest(futureDate,
                TramTime.of(8,19), false, 3, maxJourneyDuration, 1,  EnumSet.noneOf(TransportMode.class));

        @NotNull Set<Journey> results = calculator.calculateRouteAsSet(start, altrinchamInterchange, journeyRequest);
        assertFalse(results.isEmpty(), "no journeys");

    }

    @Test
    void shouldHaveJourneyAltyToKnutsford() {
        StationGroup end = knutsfordBusStation;

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(120), 1, EnumSet.allOf(TransportMode.class));
        Set<Journey> results = calculator.calculateRouteAsSet(altrinchamInterchange, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Disabled("not realistic, journey would be composite to composite")
    @Test
    void shouldHandleJourneyDirectWithinASingleComposite() {

        StationGroup piccadillyComp = stationGroupsRepository.findByName("Piccadilly Rail Station");
        List<Station> stations = new ArrayList<>(piccadillyComp.getContained());
        assertTrue(stations.size()>2);
        Station start = stations.get(0);
        Station end = stations.get(1);

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(7,30), false, 1,
                Duration.ofMinutes(120), 1,  EnumSet.noneOf(TransportMode.class));

        Set<Journey> results = calculator.calculateRouteAsSet(start, end, journeyRequest);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertFalse(journey.getStages().isEmpty()));
    }

    @Test
    void shouldHaveJourneyKnutsfordToAlty() {

        TramTime time = TramTime.of(11, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                3, Duration.ofMinutes(120), 1,  EnumSet.allOf(TransportMode.class));

        Set<Journey> results = calculator.calculateRouteAsSet(knutsfordBusStation, altrinchamInterchange, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldNotRevisitSameBusStationAltyToKnutsford() {

        TramTime travelTime = TramTime.of(15, 55);

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 2,
                maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> journeys = calculator.calculateRouteAsSet(altrinchamInterchange, knutsfordBusStation, request);

        assertFalse(journeys.isEmpty(), "no journeys");

        journeys.forEach(journey -> {
            ArrayList<IdFor<?>> seenId = new ArrayList<>();
            journey.getStages().forEach(stage -> {
                IdFor<?> actionStation = stage.getActionStation().getId();
                assertFalse(seenId.contains(actionStation), "Already seen " + actionStation + " for " + journey);
                seenId.add(actionStation);
            });
        });
    }

    @Test
    void shouldHavePiccadilyToStockportJourney() {
        int maxChanges = 2;
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 0),
                false, maxChanges, maxJourneyDuration, 3, getRequestedModes());
        Set<Journey> journeys = calculator.calculateRouteAsSet(PiccadilyStationStopA, stockportBusStation, journeyRequest);
        assertFalse(journeys.isEmpty());
        List<Journey> threeStagesOrLess = journeys.stream().filter(
                journey -> journey.getStages().size() <= (maxChanges + 1)).toList();
        assertFalse(threeStagesOrLess.isEmpty());
    }

    @Test
    void shouldReproIssueWithSlowPerformanceShudehillToBroadheathAsda() {
        int maxChanges = 3;

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,40),
                false, maxChanges, maxJourneyDuration, 3, getRequestedModes());

        Station asdaBroadhealth = stationRepository.getStationById(Station.createId("1800SJ18511"));
        assertNotNull(asdaBroadhealth);

        Set<Journey> journeys = calculator.calculateRouteAsSet(shudehillInterchange, asdaBroadhealth, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAltyToShudehill() {
        int maxChanges = 3;

        TramDate date = TramDate.of(2022,1,9);
        JourneyRequest journeyRequest = new JourneyRequest(date, TramTime.of(9,52),
                false, maxChanges, maxJourneyDuration, 3, getRequestedModes());

        Set<Journey> journeys = calculator.calculateRouteAsSet(altrinchamInterchange, shudehillInterchange, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldReproPerfIssueStockportToShudehillInterchange() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,45),
                false, 3, maxJourneyDuration, 3, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(stockportBusStation, shudehillInterchange, journeyRequest);
        assertFalse(results.isEmpty());
    }

    @Disabled("Stockport bus station is currently closed")
    @Test
    void shouldReproPerfIssueAltyToStockport3Changes() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,45),
                false, 3, maxJourneyDuration, 3, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(StopAtAltrinchamInterchange, stockportBusStation, journeyRequest);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldReproPerfIssueAltyToAirport() {
        StationGroup airport = stationGroupsRepository.findByName("Manchester Airport The Station");

        //LocalDate date =  LocalDate.of(2021, 6, 30);
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(11,11),
                false, 3, maxJourneyDuration, 3, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(altrinchamInterchange, airport, journeyRequest);
        assertFalse(results.isEmpty());
    }

}
