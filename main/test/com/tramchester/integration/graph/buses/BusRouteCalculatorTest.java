package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static com.tramchester.testSupport.reference.BusStations.PiccadilyStationStopA;
import static com.tramchester.testSupport.reference.BusStations.StopAtAltrinchamInterchange;
import static com.tramchester.testSupport.reference.KnownLocality.MIN_CHANGES;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class BusRouteCalculatorTest {

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationBusTestConfig testConfig;

    private RouteCalculatorTestFacade calculator;
    private StationGroupsRepository stationGroupsRepository;

    private final TramDate when = TestEnv.testDay();
    private MutableGraphTransaction txn;
    private Duration maxJourneyDuration;
    private StationGroup stockportCentral;
    private StationGroup altrinchamCentral;
    private StationGroup knutsfordLocality;
    private StationGroup shudehillLocality;

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
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

        stockportCentral = KnownLocality.Stockport.from(stationGroupsRepository);
        altrinchamCentral = KnownLocality.Altrincham.from(stationGroupsRepository);

        knutsfordLocality =  KnownLocality.Knutsford.from(stationGroupsRepository);
        shudehillLocality = KnownLocality.Shudehill.from(stationGroupsRepository);

        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveValidStations() {
        assertNotNull(stockportCentral);
        assertNotNull(altrinchamCentral);
        assertNotNull(knutsfordLocality);
        assertNotNull(shudehillLocality);
    }

    @Test
    void shouldHaveStockToAltyJourneyAndBackAgainOneChanges() {

        TramTime travelTime = TramTime.of(9,0);
        TramDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 1, getRequestedModes());
        List<Journey> journeysA = calculator.calculateRouteAsList(stockportCentral, altrinchamCentral, requestA);
        assertFalse(journeysA.isEmpty());

        JourneyRequest requestB = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 1, getRequestedModes());
        List<Journey> journeysB = calculator.calculateRouteAsList(altrinchamCentral, stockportCentral, requestB);
        assertFalse(journeysB.isEmpty());
    }

    @Test
    void shouldHaveStockToAltyJourneyAndBackAgainOneChangesLocalityBased() {
        StationGroup altrincham = KnownLocality.Altrincham.from(stationGroupsRepository);
        StationGroup stockport = KnownLocality.Stockport.from(stationGroupsRepository);

        TramTime travelTime = TramTime.of(9,0);
        TramDate nextMonday = TestEnv.nextMonday();

        JourneyRequest requestA = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 1, getRequestedModes());

        List<Journey> journeysA = calculator.calculateRouteAsList(stockport, altrincham, requestA);
        assertFalse(journeysA.isEmpty());

        JourneyRequest requestB = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 1, getRequestedModes());
        List<Journey> journeysB = calculator.calculateRouteAsList(altrincham, stockport, requestB);
        assertFalse(journeysB.isEmpty());
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return BusesOnly;
    }

    @Test
    void shouldHaveAltyToTownCentre() {
        TramTime time = TramTime.of(15,52);

        Location<?> manchesterCityCentre = KnownLocality.ManchesterCityCentre.from(stationGroupsRepository);

        JourneyRequest request = new JourneyRequest(when, time, false, 3,
                maxJourneyDuration, 3, getRequestedModes());
        List<Journey> journeys = calculator.calculateRouteAsList(altrinchamCentral, manchesterCityCentre, request);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveStockToAltyJourney() {

        TramTime travelTime = TramTime.of(9, 0);
        TramDate nextMonday = TestEnv.nextMonday();

        JourneyRequest request = new JourneyRequest(nextMonday, travelTime, false, 1,
                maxJourneyDuration, 3, getRequestedModes());
        List<Journey> journeys = calculator.calculateRouteAsList(stockportCentral, altrinchamCentral, request);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAltyToStockport() {

        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextMonday(),
                TramTime.of(9, 0), false, 1, maxJourneyDuration, 3, getRequestedModes());

        List<Journey> journeysMaxChanges = calculator.calculateRouteAsList(altrinchamCentral, stockportCentral, journeyRequest);

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
    void shouldHaveManchesterAirportToShudehill() {
        TramTime time = TramTime.of(11, 30);
        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

        List<Journey> results = calculator.calculateRouteAsList(KnownLocality.ManchesterAirport, KnownLocality.Shudehill, request);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveAltrinchamToBollington() {
        TramTime time = TramTime.of(10,37);

        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

        List<Journey> results = calculator.calculateRouteAsList(KnownLocality.Altrincham, KnownLocality.Bollington, request);
        assertFalse(results.isEmpty());

    }

    @Test
    void shouldHaveAltrinchamToBollingtonReproIssue() {
        TramTime time = TramTime.of(10,37);

        //TramDate date = TramDate.of(2024,2,21);
        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

//        request.setDiag(true);

        List<Journey> results = calculator.calculateRouteAsList(KnownLocality.Altrincham, KnownLocality.Bollington, request);
        assertFalse(results.isEmpty());

    }

    @Disabled("debug of specific issue only")
    @Test
    void shouldHaveAltrinchamToAirportReproIssue() {
        TramTime time = TramTime.of(10,37);

        TramDate date = TramDate.of(2024,2,21);
        JourneyRequest request = new JourneyRequest(date, time, false, MIN_CHANGES,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

//        request.setDiag(true);

        List<Journey> results = calculator.calculateRouteAsList(KnownLocality.Altrincham, KnownLocality.ManchesterAirport, request);
        assertFalse(results.isEmpty());

    }

    @Test
    void shouldHaveMacclesfieldToKnutsford() {
        TramTime time = TramTime.of(12, 30);
        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

        List<Journey> results = calculator.calculateRouteAsList(KnownLocality.Macclesfield, KnownLocality.Knutsford, request);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveMacclesfieldToShudehill() {
        TramTime time = TramTime.of(11, 30);
        JourneyRequest request = new JourneyRequest(when, time, false, 3,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, BusesOnly);

        List<Journey> results = calculator.calculateRouteAsList(KnownLocality.Macclesfield, KnownLocality.Shudehill, request);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindJourneyInFutureCorrectly() {
        // attempt to repro seen in ui where zero journeys
        StationGroup start = KnownLocality.OldfieldBrow.from(stationGroupsRepository);

        final TramDate futureDate = TestEnv.testDay().plusDays(14);
        JourneyRequest journeyRequest = new JourneyRequest(futureDate,
                TramTime.of(8,19), false, 3, maxJourneyDuration, 1,  getRequestedModes());

        @NotNull List<Journey> results = calculator.calculateRouteAsList(start, altrinchamCentral, journeyRequest);
        assertFalse(results.isEmpty(), "no journeys");

    }

    @Test
    void shouldHaveJourneyAltyToKnutsford() {
        StationGroup end = knutsfordLocality;

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(120), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(altrinchamCentral, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Disabled("not realistic, journey would be composite to composite")
    @Test
    void shouldHandleJourneyDirectWithinASingleComposite() {

        StationGroup piccadillyComp = stationGroupsRepository.findByName("Piccadilly Rail Station");
        List<Station> stations = piccadillyComp.getAllContained().stream().toList();
        assertTrue(stations.size()>2);
        Station start = stations.get(0);
        Station end = stations.get(1);

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(7,30), false, 1,
                Duration.ofMinutes(120), 1,  EnumSet.noneOf(TransportMode.class));

        List<Journey> results = calculator.calculateRouteAsList(start, end, journeyRequest);

        assertFalse(results.isEmpty());
        results.forEach(journey -> assertFalse(journey.getStages().isEmpty()));
    }

    @Test
    void shouldHaveJourneyKnutsfordToAlty() {

        TramTime time = TramTime.of(11, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                3, Duration.ofMinutes(120), 1,  getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(knutsfordLocality, altrinchamCentral, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldNotRevisitSameBusStationAltyToKnutsford() {

        TramTime travelTime = TramTime.of(15, 55);

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 2,
                maxJourneyDuration, 1, getRequestedModes());
        List<Journey> journeys = calculator.calculateRouteAsList(altrinchamCentral, knutsfordLocality, request);

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
    void shouldHavePiccadillyToStockportJourney() {
        int maxChanges = 2;
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 0),
                false, maxChanges, maxJourneyDuration, 3, getRequestedModes());
        List<Journey> journeys = calculator.calculateRouteAsList(PiccadilyStationStopA, stockportCentral, journeyRequest);
        assertFalse(journeys.isEmpty());
        List<Journey> threeStagesOrLess = journeys.stream().filter(
                journey -> journey.getStages().size() <= (maxChanges + 1)).toList();
        assertFalse(threeStagesOrLess.isEmpty());
    }

    @Test
    void shouldReproIssueWithSlowPerformanceShudehillToBroadheath() {
        int maxChanges = 3;

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,40),
                false, maxChanges, maxJourneyDuration, 3, getRequestedModes());

        StationGroup broadheath = KnownLocality.Broadheath.from(stationGroupsRepository);

        journeyRequest.setDiag(true);

        // caching currently saves about 5 seconds out of 40 (Apr 2024)
        //journeyRequest.setCachingDisabled(true);

        List<Journey> journeys = calculator.calculateRouteAsList(shudehillLocality, broadheath, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveAltyToShudehill() {
        int maxChanges = 3;

        //TramDate date = TramDate.of(2022,1,9);
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,52),
                false, maxChanges, maxJourneyDuration, 3, getRequestedModes());

        List<Journey> journeys = calculator.calculateRouteAsList(altrinchamCentral, shudehillLocality, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldReproPerfIssueStockportToShudehillInterchange() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,45),
                false, 3, maxJourneyDuration, 3, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(stockportCentral, shudehillLocality, journeyRequest);
        assertFalse(results.isEmpty());
    }

    @Disabled("Stockport bus station is currently closed")
    @Test
    void shouldReproPerfIssueAltyToStockport3Changes() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,45),
                false, 3, maxJourneyDuration, 3, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(StopAtAltrinchamInterchange, stockportCentral, journeyRequest);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldReproPerfIssueAltyToAirport() {
        StationGroup airport = KnownLocality.ManchesterAirport.from(stationGroupsRepository);

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(11,11),
                false, 3, maxJourneyDuration, 3, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(altrinchamCentral, airport, journeyRequest);
        assertFalse(results.isEmpty());
    }

}
