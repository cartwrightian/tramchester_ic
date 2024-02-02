package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.StationClosuresForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.reference.TransportMode.Walk;
import static com.tramchester.graph.graphbuild.GraphLabel.PLATFORM;
import static com.tramchester.graph.graphbuild.GraphLabel.ROUTE_STATION;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("Reworked, duplication")
class ClosedStationsDiversionsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig config;

    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;
    private final static TramDate when = TestEnv.testDay();
    private MutableGraphTransaction txn;

    private final static List<StationClosures> closedStations = Collections.singletonList(
            new StationClosuresForTest(TramStations.StPetersSquare, when, when.plusWeeks(1), true));

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new IntegrationTramClosedStationsTestConfig(closedStations, true);
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return EnumSet.of(Tram, Walk);
    }

    @Test
    void shouldHaveTheDiversionsInTheRepository() {
        StationsWithDiversionRepository repository = componentContainer.get(StationsWithDiversionRepository.class);
        assertTrue(repository.hasDiversions(Deansgate.from(stationRepository)));
        assertTrue(repository.hasDiversions(ExchangeSquare.from(stationRepository)));
        assertTrue(repository.hasDiversions(PiccadillyGardens.from(stationRepository)));
        assertTrue(repository.hasDiversions(MarketStreet.from(stationRepository)));

        assertFalse(repository.hasDiversions(Shudehill.from(stationRepository)));
    }

    @Test
    void shouldFindUnaffectedRouteNormally() {
        JourneyRequest journeyRequest = new JourneyRequest(when,TramTime.of(8,0), false,
                2, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> result = calculator.calculateRouteAsList(TramStations.Altrincham, TramStations.TraffordBar, journeyRequest);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldFindRouteWhenStartingFromClosedIfWalkPossible() {

        JourneyRequest journeyRequest = new JourneyRequest(when,TramTime.of(8,0), false,
                2, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(TramStations.StPetersSquare, TramStations.Altrincham,
                journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Connect, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(Tram, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramDifferentBury() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                10, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(TramStations.Bury, Altrincham, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrookToVictoria() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(Cornbrook, Victoria, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramVictoriaToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(Victoria, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramPiccToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(Piccadilly, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTram() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(Monsall, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrookToPicc() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(Cornbrook, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    private void validateStages(List<Journey> results) {
        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(3, stages.size(), "num stages " + result);
            assertEquals(Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
            assertEquals(Tram, stages.get(2).getMode(), "3rd mode " + result);
        });
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramDifferentRochdale() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(Rochdale, Altrincham, journeyRequest);

        assertFalse(results.isEmpty());

        validateStages(results);
    }


    @Test
    void shouldFindRouteToClosedStationViaWalkAtEnd() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 0),
                false, 3, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(TramStations.Bury, TramStations.StPetersSquare, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Test
    void shouldFindRouteWhenFromStationWithDiversionToDestBeyondClosure() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(ExchangeSquare, TramStations.Altrincham, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindRouteWhenFromStationWithDiversionToOtherDiversionStation() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(ExchangeSquare, Deansgate, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindRouteAroundClosureWhenDiversionStationIsAnInterchange() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, Duration.ofHours(2), 1, getRequestedModes());

        // change at pic gardens, an interchange
        List<Journey> results = calculator.calculateRouteAsList(Ashton, Altrincham, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldCheckForExpectedInboundRelationships() {
        List<GraphRelationshipId> foundRelationshipIds = new ArrayList<>();

        Station exchange = ExchangeSquare.from(stationRepository);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            exchange.getPlatforms().forEach(platform -> {
                GraphNode node = txn.findNode(platform);
                Stream<ImmutableGraphRelationship> iterable = node.getRelationships(txn, Direction.INCOMING, TransportRelationshipTypes.DIVERSION_DEPART);

                iterable.forEach(relationship -> foundRelationshipIds.add(relationship.getId()));
            });

        }

        assertFalse(foundRelationshipIds.isEmpty());

        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            GraphRelationship relationship = txn.getRelationshipById(foundRelationshipIds.get(0));
            GraphNode from = relationship.getStartNode(txn);
            assertTrue(from.hasLabel(ROUTE_STATION), from.getAllProperties().toString());
            GraphNode to = relationship.getEndNode(txn);
            assertTrue(to.hasLabel(PLATFORM));
        }

    }

}
