package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Altrincham;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.testSupport.TestEnv.Modes.TrainAndTram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.Eccles;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteCalculatorTest {
    private static final int TXN_TIMEOUT = 5*60;
    private static StationRepository stationRepository;
    private static RailAndTramGreaterManchesterConfig config;

    private final TramDate when = TestEnv.testDay();

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private MutableGraphTransaction txn;
    private RouteCalculatorTestFacade testFacade;

    private TramTime travelTime;
    private Duration maxDurationFromConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
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
        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        testFacade = new RouteCalculatorTestFacade(componentContainer, txn);

        travelTime = TramTime.of(8, 0);

        maxDurationFromConfig = Duration.ofMinutes(config.getMaxJourneyDuration());

    }

    @Test
    void  shouldHaveTrainsAndTramStationsInRepos() {
        assertTrue(stationRepository.hasStationId(ManchesterPiccadilly.getId()));
        assertTrue(stationRepository.hasStationId(TramStations.ExchangeSquare.getId()));
        assertTrue(stationRepository.hasStationId(TramStations.Altrincham.getId()));
    }

    @Test
    void reproIssueRochdaleToEccles() {
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        List<Journey> journeys = testFacade.calculateRouteAsList(Rochdale, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveRochdaleToStPetersSquare() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, maxDurationFromConfig,
                1, TramsOnly);

        List<Journey> journeys = testFacade.calculateRouteAsList(Rochdale, StPetersSquare, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveStPetersSquareToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0, maxDurationFromConfig,
                1, TramsOnly);

        List<Journey> journeys = testFacade.calculateRouteAsList(StPetersSquare, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveVictoriaToEccles() {
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

//        journeyRequest.setDiag(true);

        List<Journey> journeys = testFacade.calculateRouteAsList(Victoria, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveVictoriaToEcclesTrainAndTramAllowed() {
        // check if allowing all transport modes makes a difference.....
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, EnumSet.of(Tram, Train));

//        journeyRequest.setDiag(true);

        List<Journey> journeys = testFacade.calculateRouteAsList(Victoria, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveDeangateToEccles() {
        // check if failing when TramsOnly and nearby rail station
        // this works fine when only tram data loaded, but fails when tram and train is loaded
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        //journeyRequest.setDiag(true);

        List<Journey> journeys = testFacade.calculateRouteAsList(Deansgate, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveExchangeSqToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        List<Journey> journeys = testFacade.calculateRouteAsList(ExchangeSquare, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveMarketStreetToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxDurationFromConfig,
                1, TramsOnly);

        List<Journey> journeys = testFacade.calculateRouteAsList(MarketStreet, Eccles, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldReproIssueWithInvalidTimes() {
        TramTime time = TramTime.of(10,49);
        JourneyRequest request = new JourneyRequest(when, time, false, 3,
                Duration.ofMinutes(100), 1, tramAndTrain());

        // ashton west
        Station start = rail(Altrincham);
        Station dest = tram(TramStations.Ashton);

        List<Journey> journeys = testFacade.calculateRouteAsList(start, dest, request);
        assertFalse(journeys.isEmpty(), "no journeys");

    }

    @Disabled("caught at DTO mapping stage")
    @Test
    void shouldNotGenerateDuplicateJourneysForSameReqNumChanges() {

        JourneyRequest request = new JourneyRequest(when, TramTime.of(11, 45), false,
                4, maxDurationFromConfig, 3, TramsOnly);
        List<Journey> journeys =  testFacade.calculateRouteAsList(Bury, TramStations.Altrincham, request);

        assertFalse(journeys.isEmpty());

        final Set<Integer> uniqueNumChanges = journeys.stream().map(Journey::getRequestedNumberChanges).collect(Collectors.toSet());

        uniqueNumChanges.forEach(reqNumChanges -> {
            final Set<List<TransportStage<?,?>>> uniqueStages = new HashSet<>();

            journeys.stream().filter(journey -> reqNumChanges.equals(journey.getRequestedNumberChanges())).forEach(journey -> {

                final List<TransportStage<?, ?>> journeyStages = journey.getStages();
                assertFalse(uniqueStages.contains(journeyStages), journeyStages +
                        " is present multiple times in " + journeys);
                uniqueStages.add(journeyStages);
            });
        });
    }

    @Disabled("No way to detect duplicate at this level?")
    @Test
    void shouldReproIssueWithDuplicatedJourneyWhenMaxJoruneysIs5() {

        // TODO revisit this - although it does not a cause an actual issue since duplicates filtered out during mapping to DTOs

        TramTime time = TramTime.of(14,25);

        TramDate date = TramDate.of(2022, 10,14);

        // get a duplicate journey when set to 5 here...
        JourneyRequest request = new JourneyRequest(date, time, false, 1,
                Duration.ofMinutes(240), 5, EnumSet.of(Tram, Train));

        Station start = rail(Altrincham);
        Station dest = rail(Stockport);

        List<Journey> journeys = testFacade.calculateRouteAsList(start, dest, request);

        assertEquals(1, journeys.size(), "unexpected number of journeys " + journeys);

    }

    @Test
    void shouldHaveDirectAdjacentRailStations() {
        TramTime time = TramTime.of(14,25);

        TramDate date = TestEnv.testDay();

        JourneyRequest request = new JourneyRequest(date, time, false, 0,
                Duration.ofMinutes(30), 1, EnumSet.of(Train));

        List<Journey> journeys = testFacade.calculateRouteAsList(rail(Altrincham), rail(NavigationRaod), request);

        assertEquals(1, journeys.size());

        Journey journey = journeys.getFirst();

        assertEquals(1, journey.getStages().size());

        TransportStage<?, ?> stage = journey.getStages().getFirst();

        assertEquals(Duration.ofMinutes(2), stage.getDuration());
    }

    @Test
    void shouldHaveWalkBetweenAdjacentTramAndTrainStation() {
        TramTime time = TramTime.of(14,25);

        TramDate date = TestEnv.testDay();

        JourneyRequest request = new JourneyRequest(date, time, false, 0,
                Duration.ofMinutes(3), 1, EnumSet.of(Tram, Train));

        List<Journey> journeysFromTram = new ArrayList<>(testFacade.calculateRouteAsList(tram(TramStations.Altrincham),
                rail(Altrincham), request));

        List<Journey> journeysFromTrain = new ArrayList<>(testFacade.calculateRouteAsList(rail(Altrincham),
                tram(TramStations.Altrincham), request));

        assertEquals(1, journeysFromTram.size());
        assertEquals(1, journeysFromTrain.size());

        Journey fromTramJoruney = journeysFromTram.getFirst();
        List<TransportStage<?, ?>> fromTramStages = fromTramJoruney.getStages();
        assertEquals(1, fromTramStages.size());

        Journey fromTrainJoruney = journeysFromTram.getFirst();
        List<TransportStage<?, ?>> fromTrainStages = fromTrainJoruney.getStages();
        assertEquals(1, fromTrainStages.size());

        TransportStage<?, ?> fromTram = fromTramStages.getFirst();
        assertEquals(Connect, fromTram.getMode());
        assertEquals(Duration.ofSeconds(51), fromTram.getDuration());

        TransportStage<?, ?> fromTrain = fromTrainStages.getFirst();
        assertEquals(Connect, fromTrain.getMode());
        assertEquals(Duration.ofSeconds(51), fromTrain.getDuration());
    }

    @Test
    void shouldTakeDirectTrainWhenAvailable() {
        TramTime time = TramTime.of(14,25);

        //TramDate date = TramDate.of(2022, 10,14);
        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 1, EnumSet.of(Tram, Train));

        Station start = rail(Altrincham);
        Station dest = rail(Stockport);

        List<Journey> journeys = testFacade.calculateRouteAsList(start, dest, request);
        assertEquals(1, journeys.size(), "unexpected number of journeys " + journeys);

        Journey journey = journeys.getFirst();

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(1, stages.size(), "too many stages " + journey);

        TransportStage<?, ?> stage = stages.getFirst();
        assertEquals(stage.getMode(), Train, "wrong second stage for " + stages);
        assertEquals(Duration.ofMinutes(16), stage.getDuration());
    }

    @Test
    void shouldTakeDirectTrainWhenStartAtTramStationNextToStationAndTramDisabled() {

        // repro issues - coming back with a tram to nav road and then a change to the train instead of
        // direct from altrincham stations
        // Works fine for direct from altrincham rail, so seems to be an issue with crossing to the train station

        TramTime time = TramTime.of(10,30);

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 3, EnumSet.of(Train));

        Station start = tram(TramStations.Altrincham); // TRAM
        Station dest = rail(Stockport);

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsList(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.getFirst();

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(2, stages.size(),  "too many stages " + journeys);
        assertEquals(stages.get(0).getMode(), Connect, "wrong first stage for " + stages);
        assertEquals(stages.get(1).getMode(), Train, "wrong second stage for " + stages);

    }

    @Test
    void shouldTakeDirectTrainWhenStartAtTramStationNextToStation() {

        // repro issues - coming back with a tram to nav road and then a change to the train instead of
        // direct from altrincham stations
        // Works fine for direct from altrincham rail, so seems to be an issue with crossing to the train station

        TramTime time = TramTime.of(10,30);

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 3, TrainAndTram);

        Station start = tram(TramStations.Altrincham); // TRAM
        Station dest = rail(NavigationRaod); // TRAIN

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsList(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.getFirst();

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(2, stages.size(),  "too many stages " + journeys);
        assertEquals(stages.get(0).getMode(), Connect, "wrong first stage for " + stages);
        assertEquals(stages.get(1).getMode(), Train, "wrong second stage for " + stages);

    }


    @Disabled("Not a realistic scenario? start from a tram station but select train only?")
    @Test
    void shouldTakeDirectTrainViaWalkWhenOnlyTrainModeSelected() {
        TramTime time = TramTime.of(14,25);

        EnumSet<TransportMode> trainOnly = EnumSet.of(Train);

        TramDate date = TramDate.of(2022, 10,14);
        JourneyRequest request = new JourneyRequest(date, time, false, 1,
                Duration.ofMinutes(240), 1, trainOnly);

        Station start = tram(TramStations.Altrincham);
        Station dest = rail(Stockport);

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsList(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.getFirst();

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(2, stages.size(),  "too many stages " + journey);
        assertEquals(stages.get(0).getMode(), Connect, "wrong first stage for " + stages);
        assertEquals(stages.get(1).getMode(), Train, "wrong second stage for " + stages);

    }

    @Test
    void shouldHaveStockportToManPiccViaRail() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 1,
                Duration.ofMinutes(30), 1, tramAndTrain());

        atLeastOneDirect(request, rail(Stockport), rail(ManchesterPiccadilly), Train);
    }


    @Test
    void shouldHaveManPiccToStockportViaRail() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, tramAndTrain());

        atLeastOneDirect(request, rail(ManchesterPiccadilly), rail(Stockport), Train);
    }

    @Test
    void shouldNotHaveManPiccToStockportWhenTramOnly() {

        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, EnumSet.of(Tram));

        List<Journey> journeys = testFacade.calculateRouteAsList(rail(ManchesterPiccadilly), rail(Stockport), request);
        assertTrue(journeys.isEmpty());

    }

    @Test
    void shouldHaveAltyToStPetersSquareViaTram() {
        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, tramAndTrain());

        atLeastOneDirect(request, tram(TramStations.Altrincham), tram(TramStations.StPetersSquare), Tram);
    }

    @Test
    void shouldHaveNeighboursFromConfig() {
        NeighboursRepository neighboursRepository = componentContainer.get(NeighboursRepository.class);

        Station eastDidsburyRail = RailStationIds.EastDidsbury.from(stationRepository);
        Station eastDidsburyTram = TramStations.EastDidsbury.from(stationRepository);

        Set<Station> neighbours = neighboursRepository.getNeighboursFor(eastDidsburyTram.getId());
        assertEquals(1, neighbours.size());

        assertTrue(neighbours.contains(eastDidsburyRail));

        neighbours = neighboursRepository.getNeighboursFor(eastDidsburyRail.getId());
        assertEquals(1, neighbours.size());

        assertTrue(neighbours.contains(eastDidsburyTram));
    }

    @Test
    void shouldHaveWalKFromDidsburyTramToDidsburyTrain() {
        JourneyRequest request = new JourneyRequest(when, travelTime, false, 0,
                Duration.ofMinutes(30), 1, tramAndTrain());

        Station start = tram(TramStations.EastDidsbury);
        Station dest = RailStationIds.EastDidsbury.from(stationRepository);
        List<Journey> journeys = testFacade.calculateRouteAsList(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).toList();
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);

        direct.forEach(journey -> journey.getStages().forEach(stage -> assertEquals(Connect, stage.getMode(),
                "Mode wrong for journey " + journey + " for request " + request)));

    }

    @Test
    void shouldBuryToStockportViaTramAndTrain() {
        JourneyRequest request = new JourneyRequest(when, travelTime, false, 3,
                Duration.ofMinutes(110), 1, tramAndTrain());

        List<Journey> journeys = testFacade.calculateRouteAsList(tram(TramStations.Bury), rail(Stockport), request);
        assertFalse(journeys.isEmpty(),"no journeys");
    }

    @Test
    void shouldHaveMultistageTest() {
        // reproduces failing scenario from Acceptance tests
        //   TramTime planTime = TramTime.of(10,0);
        //        desiredJourney(appPage, altrincham, TramStations.ManAirport.getName(), when, planTime, false);

        JourneyRequest request = new JourneyRequest(when, TramTime.of(10,0), false, 2,
                Duration.ofMinutes(110), 1, tramAndTrain());

        List<Journey> results = testFacade.calculateRouteAsList(TramStations.Altrincham, TramStations.ManAirport, request);
        assertFalse(results.isEmpty());
    }

    private Station rail(RailStationIds railStation) {
        return railStation.from(stationRepository);
    }

    private Station tram(TramStations tramStation) {
        return tramStation.from(stationRepository);
    }

    private void atLeastOneDirect(JourneyRequest request, Station start, Station dest, TransportMode mode) {
        List<Journey> journeys = testFacade.calculateRouteAsList(start, dest, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).toList();
        assertFalse(direct.isEmpty(), "No direct from " + start + " to " + dest);

        direct.forEach(journey -> journey.getStages().forEach(stage -> assertEquals(mode, stage.getMode(),
                "Mode wrong for journey " + journey + " for request " + request)));
    }


    private EnumSet<TransportMode> tramAndTrain() {
        return EnumSet.of(Tram, Train, RailReplacementBus);
    }



}
