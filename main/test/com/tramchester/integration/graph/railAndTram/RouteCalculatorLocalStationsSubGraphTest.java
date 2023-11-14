package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
class RouteCalculatorLocalStationsSubGraphTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private TramDate when;

    private static final List<IdFor<Station>> stationIds = TestEnv.asList(
            TramStations.Altrincham,
            TramStations.NavigationRoad,
            TramStations.Timperley,
            RailStationIds.Altrincham,
            NavigationRaod,
            Stockport);

    private MutableGraphTransaction txn;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;
    private RouteCalculatorTestFacade testFacade;
    private TramTime time;
    private IdFor<Agency> northern;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorLocalStationsSubGraphTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        stationIds.forEach(graphFilter::addStation);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        txn = database.beginTx();

        testFacade = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);

        northern = TrainOperatingCompanies.NT.getAgencyId();

        time = TramTime.of(14,50);
        when = TestEnv.testDay();
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void validateHaveExpectedRouteStationInRepositoryChesterToStockport() {
        RailRouteId railRouteId = new RailRouteId(RailStationIds.Chester.getId(), Stockport.getId(), northern, 1);

        Route chesterToStockport = routeRepository.getRouteById(railRouteId);
        assertNotNull(chesterToStockport);

        RouteStation navigationRouteStation = stationRepository.getRouteStationById(RouteStationId.createId(railRouteId, NavigationRaod.getId()));
        assertNotNull(navigationRouteStation);

        RouteStation altrinchamRouteStation = stationRepository.getRouteStationById(RouteStationId.createId(railRouteId, Altrincham.getId()));
        assertNotNull(altrinchamRouteStation);
    }

    @Test
    void validateHaveExpectedRouteStationInRepositoryStockportToAltrincham() {
        RailRouteId railRouteId = new RailRouteId(Stockport.getId(), Chester.getId(), northern, 1);

        Route route = routeRepository.getRouteById(railRouteId);
        assertNotNull(route);

        RouteStation navigationRouteStation = stationRepository.getRouteStationById(RouteStationId.createId(railRouteId, NavigationRaod.getId()));
        assertNotNull(navigationRouteStation);

        RouteStation altrinchamRouteStation = stationRepository.getRouteStationById(RouteStationId.createId(railRouteId, Altrincham.getId()));
        assertNotNull(altrinchamRouteStation);
    }

    @Test
    void shouldHaveWalkBetweenAdjacentTramAndTrainStation() {

        JourneyRequest request = new JourneyRequest(when, time, false, 0,
                Duration.ofMinutes(3), 1, getRequestedModes());

        List<Journey> journeysFromTram = new ArrayList<>(testFacade.calculateRouteAsSet(tram(TramStations.Altrincham),
                rail(Altrincham), request));

        assertEquals(1, journeysFromTram.size());

        Journey fromTramJoruney = journeysFromTram.get(0);
        List<TransportStage<?, ?>> fromTramStages = fromTramJoruney.getStages();
        assertEquals(1, fromTramStages.size());

        TransportStage<?, ?> fromTram = fromTramStages.get(0);
        assertEquals(Connect, fromTram.getMode());
        assertEquals(Duration.ofMinutes(1), fromTram.getDuration());
    }

    @Test
    void shouldHaveWalkBetweenAdjacentTrainAndTramStation() {

        JourneyRequest request = new JourneyRequest(when, time, false, 0,
                Duration.ofMinutes(3), 1, getRequestedModes());

        List<Journey> journeysFromTrain = new ArrayList<>(testFacade.calculateRouteAsSet(rail(Altrincham),
                tram(TramStations.Altrincham), request));

        assertEquals(1, journeysFromTrain.size());

        Journey fromTrainJoruney = journeysFromTrain.get(0);
        List<TransportStage<?, ?>> fromTrainStages = fromTrainJoruney.getStages();
        assertEquals(1, fromTrainStages.size());

        TransportStage<?, ?> fromTrain = fromTrainStages.get(0);
        assertEquals(Connect, fromTrain.getMode());
        assertEquals(Duration.ofMinutes(1), fromTrain.getDuration());
    }

    @Test
    void shouldTakeDirectTrainAltrinchamToStockportWhenAvailable() {

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 1, getRequestedModes());

        Station start = rail(Altrincham);
        Station dest = rail(Stockport);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertEquals(1, journeys.size(), "unexpected number of journeys " + journeys);

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size(), "too many stages " + journey);
            assertEquals(stages.get(0).getMode(), Train, "wrong second stage for " + stages);
        });
    }

    @Test
    void shouldTakeDirectTrainNavigationRoadToStockportWhenAvailable() {

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 1, getRequestedModes());

        Station start = rail(NavigationRaod);
        Station dest = rail(Stockport);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertEquals(1, journeys.size(), "unexpected number of journeys " + journeys);

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size(), "too many stages " + journey);
            assertEquals(stages.get(0).getMode(), Train, "wrong second stage for " + stages);
        });
    }

    @Test
    void shouldTakeDirectTrainToNavigationRoadWhenAvailable() {

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(30), 1, getRequestedModes());

        Station start = rail(Altrincham);
        Station dest = rail(NavigationRaod);

        Set<Journey> journeys = testFacade.calculateRouteAsSet(start, dest, request);
        assertEquals(1, journeys.size(), "unexpected number of journeys " + journeys);

        journeys.forEach(journey -> {
            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size(), "too many stages " + journey);
            assertEquals(stages.get(0).getMode(), Train, "wrong first stage for " + stages);
        });

    }

    @Test
    void shouldTakeDirectTrainAltrinchamTramToStockportRail() {

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 1, getRequestedModes());

        Station start = tram(TramStations.Altrincham); // TRAM
        Station dest = rail(Stockport);

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsSet(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.get(0);

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(2, stages.size(),  "too many stages " + journeys);
        assertEquals(stages.get(0).getMode(), Connect, "wrong first stage for " + stages);
        assertEquals(stages.get(1).getMode(), Train, "wrong second stage for " + stages);

    }

    @Test
    void shouldTakeDirectTrainThenTramToTimperley() {

        TramTime fromStockportTime = TramTime.of(14,5);
        JourneyRequest request = new JourneyRequest(when, fromStockportTime, false, 3,
                Duration.ofMinutes(240), 1, getRequestedModes());

        Station start = rail(Stockport); // TRAM
        Station dest = tram(TramStations.Timperley);

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsSet(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.get(0);

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(3, stages.size(),  "too many stages " + journeys);
        assertEquals(stages.get(0).getMode(), Train, "wrong first stage for " + journey);
        assertEquals(stages.get(1).getMode(), Connect, "wrong second stage for " + journey);
        assertEquals(stages.get(2).getMode(), Tram, "wrong third stage for " + journey);
    }

    @Test
    void shouldTakeDirectTrainNavigationRoadTramToStockportRail() {

        JourneyRequest request = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(240), 3, getRequestedModes());

        Station start = tram(TramStations.NavigationRoad); // TRAM
        Station dest = rail(Stockport);

        List<Journey> journeys = new ArrayList<>(testFacade.calculateRouteAsSet(start, dest, request));
        assertFalse(journeys.isEmpty(), "no journeys");

        Journey journey = journeys.get(0);

        List<TransportStage<?, ?>> stages = journey.getStages();
        assertEquals(2, stages.size(),  "too many stages " + journeys);
        assertEquals(stages.get(0).getMode(), Connect, "wrong first stage for " + stages);
        assertEquals(stages.get(1).getMode(), Train, "wrong second stage for " + stages);

    }


    private EnumSet<TransportMode> getRequestedModes() {
        return EnumSet.of(Train, Tram);
    }

    private static class SubgraphConfig extends RailAndTramGreaterManchesterConfig {
        public SubgraphConfig() {
            super("subgraph_trainTramLocal_tramchester.db");
        }
    }

    private Station rail(RailStationIds railStation) {
        return railStation.from(stationRepository);
    }

    private Station tram(TramStations tramStation) {
        return tramStation.from(stationRepository);
    }

}
