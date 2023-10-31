package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.*;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphRelationshipId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.StationClosuresForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("Duplicates")
class SubgraphClosedStationsDiversionsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private static final List<TramStations> centralStations = Arrays.asList(
            Cornbrook,
            Deansgate,
            StPetersSquare,
            ExchangeSquare,
            Victoria,
            MarketStreet,
            Shudehill,
            PiccadillyGardens,
            Piccadilly,
            Monsall,
            NewIslington);

    private static GraphQuery graphQuery;
    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;
    private final static TramDate when = TestEnv.testDay();
    private GraphTransaction txn;

    private final static List<StationClosures> closedStations = Collections.singletonList(
            new StationClosuresForTest(PiccadillyGardens, when, when.plusWeeks(1), true));
    private Duration maxJourneyDuration;
    private int maxChanges;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        TramchesterConfig config = new IntegrationTramClosedStationsTestConfig(closedStations, true);
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(SubgraphClosedStationsDiversionsTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
        graphQuery = componentContainer.get(GraphQuery.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        centralStations.forEach(station -> graphFilter.addStation(station.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();

        /////////////
        // TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        maxJourneyDuration = Duration.ofMinutes(30);
        maxChanges = 2;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return EnumSet.noneOf(TransportMode.class);
    }

    @Test
    void shouldHaveTheDiversionsInTheRepository() {
        StationsWithDiversionRepository repository = componentContainer.get(StationsWithDiversionRepository.class);
        assertTrue(repository.hasDiversions(Piccadilly.from(stationRepository)));
        assertTrue(repository.hasDiversions(StPetersSquare.from(stationRepository)));
        assertTrue(repository.hasDiversions(MarketStreet.from(stationRepository)));

        assertTrue(repository.hasDiversions(Shudehill.from(stationRepository)));
        assertFalse(repository.hasDiversions(Monsall.from(stationRepository)));
    }


    @Test
    void shouldFindRouteAroundCloseBackOnToTramMonsallToPiccadilly() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Monsall, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(3, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Tram, stages.get(1).getMode(), "2nd mode " + result);
            assertEquals(TransportMode.Connect, stages.get(2).getMode(), "3rd mode " + result);
        });
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramNewIslingtonToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(NewIslington, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        validateStages(results);
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrookToPicc() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Test
    void shouldFindMonsallToPiccadilly() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Monsall, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(3, stages.size(), "num stages " + result);
            TransportStage<?, ?> firstStage = stages.get(0);
            assertEquals(TransportMode.Tram, firstStage.getMode(), "1st mode " + result);
            assertEquals(Victoria.getId(), firstStage.getLastStation().getId());

            assertEquals(TransportMode.Connect, stages.get(2).getMode(), "3rd mode " + result);
        });
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramNewIslingtonToMonsall() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                4, maxJourneyDuration, 1, getRequestedModes());

        // TODO Correct number of changes limit?

        journeyRequest.setDiag(true);

        Set<Journey> results = calculator.calculateRouteAsSet(NewIslington, Monsall, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(3, stages.size(), "num stages " + result);
        });
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrooktoMonsall() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(Piccadilly, Monsall, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(3, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Connect, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Tram, stages.get(1).getMode(), "2nd mode " + result);
            assertEquals(TransportMode.Tram, stages.get(2).getMode(), "3rd mode " + result);
        });
    }

    private void validateStages(Set<Journey> results) {
        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(3, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
            assertEquals(TransportMode.Tram, stages.get(2).getMode(), "3rd mode " + result);
        });
    }

    @Test
    void shouldFindRouteWalkAtStart() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 0),
                false, 3, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(PiccadillyGardens, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Connect, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Tram, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Test
    void shouldFindRouteToClosedStationViaWalkAtEnd() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8, 0),
                false, 3, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(Cornbrook, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            final List<TransportStage<?, ?>> stages = result.getStages();
            assertEquals(2, stages.size(), "num stages " + result);
            assertEquals(TransportMode.Tram, stages.get(0).getMode(), "1st mode " + result);
            assertEquals(TransportMode.Connect, stages.get(1).getMode(), "2nd mode " + result);
        });
    }

    @Test
    void shouldFindRouteWhenFromStationWithDiversionToOtherDiversionStation() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(ExchangeSquare, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Disabled("All central also interchanges?")
    @Test
    void shouldCheckForExpectedInboundRelationships() {
        List<GraphRelationshipId> foundRelationshipIds = new ArrayList<>();

        Station notAnInterchange = MarketStreet.from(stationRepository);

        try (GraphTransaction txn = database.beginTx()) {
            notAnInterchange.getPlatforms().forEach(platform -> {
                GraphNode node = graphQuery.getPlatformNode(txn, platform);
                Stream<GraphRelationship> iterable = node.getRelationships(txn, Direction.INCOMING, TransportRelationshipTypes.DIVERSION_DEPART);

                iterable.forEach(relationship -> foundRelationshipIds.add(relationship.getId()));
            });

        }

        assertFalse(foundRelationshipIds.isEmpty());

        try (GraphTransaction txn = database.beginTx()) {
            GraphRelationship relationship = txn.getRelationshipById(foundRelationshipIds.get(0));
            GraphNode from = relationship.getStartNode(txn);
            assertTrue(from.hasLabel(ROUTE_STATION), from.getAllProperties().toString());
            GraphNode to = relationship.getEndNode(txn);
            assertTrue(to.hasLabel(PLATFORM));
        }

    }

    @Test
    void shouldCheckIfDiversionFromPiccToStPetersSquare()  {
        List<GraphRelationshipId> foundRelationshipIds = new ArrayList<>();

        Station piccadilly = Piccadilly.from(stationRepository);

        try (GraphTransaction txn = database.beginTx()) {
            GraphNode stationNode = graphQuery.getStationNode(txn, piccadilly);

            Stream<GraphRelationship> iterable = stationNode.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.DIVERSION);

            iterable.forEach(relationship -> foundRelationshipIds.add(relationship.getId()));
        }

        assertFalse(foundRelationshipIds.isEmpty(), "No diversions found");

        AtomicInteger count = new AtomicInteger(0);

        try (GraphTransaction txn = database.beginTx()) {
            foundRelationshipIds.forEach(foundId -> {
                GraphRelationship relationship = txn.getRelationshipById(foundId);
                GraphNode to = relationship.getEndNode(txn);
                assertTrue(to.hasLabel(STATION));
                IdFor<Station> stationId = GraphProps.getStationId(to);
                if (StPetersSquare.getId().equals(stationId)) {
                    count.getAndIncrement();
                }
            });
        }

        assertEquals(1, count.get());

    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_central_with_closure_trams.dot"), StPetersSquare.fake(), 100, true);
    }

}
