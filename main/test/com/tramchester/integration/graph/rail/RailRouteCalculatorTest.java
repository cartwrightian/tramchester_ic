package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
public class RailRouteCalculatorTest {
    private static final int TXN_TIMEOUT = 5*60;
    private static StationRepository stationRepository;

    private final TramDate when = TestEnv.testDay();
    private final TramDate afterEngineering = when.plusWeeks(2);

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private ImmutableGraphTransactionNeo4J txn;
    private RouteCalculatorTestFacade testFacade;
    private Station stockport;
    private Station manchesterPiccadilly;
    private Station altrincham;
//    private Station londonEuston;
    private Station macclesfield;
    private TramTime travelTime;
//    private Station stokeOnTrent;
//    private Station miltonKeynesCentral;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.GreaterManchester);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        stationRepository = componentContainer.get(StationRepository.class);
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterEach
    void afterAllEachTestsHasRun() {
        txn.close();
    }

    @AfterAll
    static void afterAllTestsRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        testFacade = new RouteCalculatorTestFacade(componentContainer, txn);

        stockport = Stockport.from(stationRepository);
        manchesterPiccadilly = stationRepository.getStationById(ManchesterPiccadilly.getId());
        altrincham = stationRepository.getStationById(Altrincham.getId());


//        londonEuston = stationRepository.getStationById(LondonEuston.getId());
        macclesfield = stationRepository.getStationById(Macclesfield.getId());
//        stokeOnTrent = stationRepository.getStationById(StokeOnTrent.getId());
//        miltonKeynesCentral = stationRepository.getStationById(MiltonKeynesCentral.getId());

        travelTime = TramTime.of(8, 0);
    }

    @Test
    void shouldHaveStockportToManPicc() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 1,
                Duration.ofMinutes(30), 1, getRequestedModes());

        atLeastOneDirect(request, stockport, manchesterPiccadilly);
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return EnumSet.of(TransportMode.Train);
    }

    @Test
    void shouldHaveManPiccToStockport() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, getRequestedModes());

        atLeastOneDirect(request, manchesterPiccadilly, stockport);
    }

    @Test
    void shouldHaveManPiccToMacclesfield() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(45), 1, getRequestedModes());

        atLeastOneDirect(request, manchesterPiccadilly, macclesfield);
    }

//    @Test
//    void shouldHaveManPiccToMiltonKeynesCentral() {
//        JourneyRequest request = new JourneyRequest(afterEngineering, travelTime, false, 0,
//                Duration.ofMinutes(120), 1, getRequestedModes());
//
//        atLeastOneDirect(request, manchesterPiccadilly, miltonKeynesCentral);
//    }

//    @Test
//    void shouldHaveMiltonKeynesToManchester() {
//        JourneyRequest request = new JourneyRequest(afterEngineering, travelTime, false, 0,
//                Duration.ofMinutes(120), 1, getRequestedModes());
//
//        atLeastOneDirect(request, miltonKeynesCentral, manchesterPiccadilly);
//    }

//    @Test
//    void shouldHaveManPiccToStoke() {
//        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
//                Duration.ofMinutes(80), 1, getRequestedModes());
//
//        atLeastOneDirect(request, manchesterPiccadilly, stokeOnTrent);
//    }

    @Test
    void shouldHaveAltrinchamToStockport() {

        TramTime time = TramTime.of(8,45);
        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(45), 1, getRequestedModes());

        atLeastOneDirect(request, altrincham, stockport);
    }

//    @Test
//    void shouldHaveManchesterToLondonEuston() {
//
//        JourneyRequest request = new JourneyRequest(afterEngineering, travelTime, false, 0,
//                Duration.ofMinutes(240), 3, getRequestedModes());
//
//        atLeastOneDirect(request, manchesterPiccadilly, londonEuston);
//    }

    @Test
    void shouldHaveHaleToKnutsford() {
        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 1,
                Duration.ofMinutes(30), 1, getRequestedModes());

        List<Journey> directs = atLeastOneDirect(request, Hale.getId(), Knutsford.getId());

        Journey result = directs.getFirst();

        assertEquals(4, result.getPath().size(), result.getPath().toString());

        List<TransportStage<?, ?>> stages = result.getStages();

        assertEquals(1, stages.size());
        TransportStage<?, ?> stage = stages.getFirst();

        assertEquals(TransportMode.Train, stage.getMode());
        assertEquals(2, stage.getPassedStopsCount());
    }

    @Test
    void shouldHaveKnutsfordToHale9am() {
        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 1,
                Duration.ofMinutes(120), 1, getRequestedModes());

        atLeastOneDirect(request, Knutsford.getId(), Hale.getId());
    }

    @Disabled("outside of GM")
    @Test
    void shouldFindCorrectNumberOfJourneys() {

        // TODO takes a *long* time to eliminate the first journey option. > 11 seconds

        TramTime travelTime = TramTime.of(11,40);

        JourneyRequest journeyRequest = new JourneyRequest(when, travelTime, false, 3,
                Duration.ofMinutes(3*60), 2, getRequestedModes());

        //journeyRequest.setDiag(true);

        List<Journey> results = testFacade.calculateRouteAsList(Derby.getId(), Altrincham.getId(), journeyRequest);

        assertEquals(1, results.size(), results.toString());
    }

    @Disabled("outside of GM")
    @Test
    void shouldHaveSimpleJourneyEustonToManchester() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(afterEngineering, travelTime, false, 0,
                Duration.ofMinutes(3*60), 3, getRequestedModes());
        List<Journey> journeys = testFacade.calculateRouteAsList(RailStationIds.LondonEuston.getId(),
                ManchesterPiccadilly.getId(),
                request);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size());

            TransportStage<?, ?> trainStage = stages.get(0);

            assertEquals(TransportMode.Train, trainStage.getMode());
            final int passedStopsCount = trainStage.getPassedStopsCount();
            assertEquals(3, passedStopsCount, trainStage.toString());

            List<StopCall> callingPoints = trainStage.getCallingPoints();
            final int numCallingPoints = callingPoints.size();
            assertTrue(numCallingPoints==3 || numCallingPoints==4, callingPoints.toString());

            if (numCallingPoints==4) {
                // milton K -> Stoke -> Macclesfield -> Stockport
                assertEquals(MiltonKeynesCentral.getId(), callingPoints.get(0).getStationId());
                assertEquals(StokeOnTrent.getId(), callingPoints.get(1).getStationId());
                assertEquals(Macclesfield.getId(), callingPoints.get(2).getStationId());
                assertEquals(Stockport.getId(), callingPoints.get(3).getStationId());
            } else {
                // crewe -> wilmslow -> stockport OR Milton Keynes, Stoke, stockport
                //assertEquals(Crewe.getId(), callingPoints.get(0).getStationId());
                //assertEquals(Wilmslow.getId(), callingPoints.get(1).getStationId());
                assertEquals(Stockport.getId(), callingPoints.get(2).getStationId());
            }
        });
    }

//    @Disabled("performance")
//    @Test
//    void shouldHaveAltrinchamToLondonEuston() {
//        TramTime travelTime = TramTime.of(8, 0);
//
//        JourneyRequest request = new JourneyRequest(when, travelTime, false, 2,
//                Duration.ofMinutes(240), 1, getRequestedModes());
//
//        atLeastOneDirect(request, altrincham, londonEuston);
//    }

    private void atLeastOneDirect(JourneyRequest request, Station start, Station dest) {
        atLeastOneDirect(request, start.getId(), dest.getId());
    }

    private List<Journey> atLeastOneDirect(JourneyRequest request, IdFor<Station> start, IdFor<Station> dest) {
        List<Journey> journeys = testFacade.calculateRouteAsList(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).toList();
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest + " for " + request);
        return direct;
    }


}
