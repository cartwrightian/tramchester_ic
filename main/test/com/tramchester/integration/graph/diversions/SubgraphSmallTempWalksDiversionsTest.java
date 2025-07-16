package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.AddDiversionsForClosedGraphBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.StationsWithDiversion;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.CentralStationsSubGraph;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("WIP")
class SubgraphSmallTempWalksDiversionsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;
    public static final Duration STPETERS_PICC_WALK = Duration.ofMinutes(10).plusSeconds(36);
    private static final Duration STPETERS_PICC_GARDENS_WALK = Duration.ofMinutes(6).plusSeconds(1);

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramClosedStationsTestConfig config;

    // TODO will need to find another data/approach once works complete
    private final static TramDate when = TestEnv.testDay();
    private static final DateRange range = DateRange.of(when.plusDays(1), when.plusDays(5));

    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;
    private ImmutableGraphTransactionNeo4J txn;
    private Duration maxJourneyDuration;
    private int maxChanges;
    private StationsWithDiversionRepository diversionRepository;
    private int maxNumResults;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {

        /// TODO
        List<TemporaryStationsWalkIds> walks = Collections.emptyList();


        IntegrationTramTestConfig configWithwalks = new IntegrationTramTestConfig(Collections.emptyList(), IntegrationTramTestConfig.Caching.Disabled,
                walks);

        Optional<GTFSSourceConfig> findTFGM = configWithwalks.getGTFSDataSource().stream().
                filter(source -> source.getDataSourceId().equals(DataSourceID.tfgm)).findFirst();

        GTFSSourceConfig tfgmConfig = findTFGM.orElseThrow();

        config = new SubgraphConfig(tfgmConfig.getTemporaryStationWalks());

        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(CentralStationsSubGraph::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
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
        // trigger full build of graph DB
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);

        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        diversionRepository = componentContainer.get(StationsWithDiversionRepository.class);

        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        maxNumResults = config.getMaxNumResults();
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        maxChanges = 2;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveTheDiversionsInTheRepository() {
        assertTrue(diversionRepository.hasDiversions(StPetersSquare.from(stationRepository)));
        assertTrue(diversionRepository.hasDiversions(Piccadilly.from(stationRepository)));
        assertTrue(diversionRepository.hasDiversions(PiccadillyGardens.from(stationRepository)));

        assertFalse(diversionRepository.hasDiversions(Shudehill.from(stationRepository)));

        // try again but via load from DB, instead of during population
        StationsWithDiversionRepository again = recreateRepository();

        assertTrue(again.hasDiversions(StPetersSquare.from(stationRepository)));
        assertTrue(again.hasDiversions(Piccadilly.from(stationRepository)));
        assertTrue(diversionRepository.hasDiversions(PiccadillyGardens.from(stationRepository)));
        assertFalse(again.hasDiversions(Shudehill.from(stationRepository)));
    }

    @Test
    void shouldHaveCorrectDateRangesForDiversion() {

        hasDateRange(StPetersSquare.from(stationRepository), range, diversionRepository);
        hasDateRange(Piccadilly.from(stationRepository), range, diversionRepository);
        hasDateRange(PiccadillyGardens.from(stationRepository), range, diversionRepository);

        // try again but via load from DB, instead of during population
        StationsWithDiversionRepository again = recreateRepository();
        hasDateRange(StPetersSquare.from(stationRepository), range, again);
        hasDateRange(Piccadilly.from(stationRepository), range, again);
        hasDateRange(PiccadillyGardens.from(stationRepository), range, diversionRepository);

    }

    private void hasDateRange(Station station, DateRange expected, StationsWithDiversionRepository repository) {
        final Set<DateTimeRange> ranges = repository.getDateTimeRangesFor(station);

        Set<DateRange> dateRanges = ranges.stream().map(DateTimeRange::getDateRange).collect(Collectors.toSet());
        assertTrue(dateRanges.contains(expected));

        ranges.forEach(range -> assertTrue(range.getTimeRange().allDay(), "Expected all day time range for " + range));
    }

    @Test
    void shouldHaveExpectedRouteToRouteCostsForDiversionStPetersToPicc() {

        Location<?> start = StPetersSquare.from(stationRepository);
        Location<?> destination = Piccadilly.from(stationRepository);

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(6,0), TramTime.of(23,55));
        EnumSet<TransportMode> mode = EnumSet.of(TransportMode.Tram);

        int costs = getPossibleMinChanges(start, destination, mode, when.plusDays(1), timeRange);

        assertEquals(0, costs);
    }

    @Test
    void shouldHaveExpectedRouteToRouteCostsForDiversionStPetersToPiccGardens() {

        Location<?> start = StPetersSquare.from(stationRepository);
        Location<?> destination = PiccadillyGardens.from(stationRepository);

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(6,0), TramTime.of(23,55));
        EnumSet<TransportMode> mode = EnumSet.of(TransportMode.Tram);

        int costs = getPossibleMinChanges(start, destination, mode, when.plusDays(1), timeRange);

        assertEquals(0, costs);
    }

    private int getPossibleMinChanges(Location<?> being, Location<?> end, EnumSet<TransportMode> modes, TramDate date, TimeRange timeRange) {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

        JourneyRequest journeyRequest = new JourneyRequest(date, timeRange.getStart(), false, JourneyRequest.MaxNumberOfChanges.of(1),
                Duration.ofMinutes(120), 1, modes);
        return routeToRouteCosts.getNumberOfChanges(being, end, journeyRequest, timeRange);
    }

    @Test
    void shouldHaveJourneyFromPiccGardensToPiccadilly() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(PiccadillyGardens, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            assertEquals(1, result.getStages().size(), result.toString());
            assertEquals(TransportMode.Tram, result.getStages().getFirst().getMode());
        });
    }

    @Test
    void shouldFindPiccGardensToPicc() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 2,
                maxJourneyDuration, maxNumResults, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(PiccadillyGardens, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty());

        Location<?> marketStreet = MarketStreet.from(stationRepository);
        List<Journey> incorrect = results.stream().
                filter(journey -> journey.getPath().contains(marketStreet)).
                toList();

        assertTrue(incorrect.isEmpty(), incorrect.toString());
    }

    @Test
    void shouldFindStPetersToPiccadilly() {
        // this test attempts to repro an issue where the journey becomes
        // StPeters->PiccGardens->MarketStreet->Picc (via Picc Gardens again...!)

        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 2,
                maxJourneyDuration, maxNumResults, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(StPetersSquare, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty());

        Location<?> marketStreet = MarketStreet.from(stationRepository);
        List<Journey> incorrect = results.stream().
                filter(journey -> journey.getPath().contains(marketStreet)).
                toList();

        assertTrue(incorrect.isEmpty(), incorrect.toString());

        results.forEach(result -> {
            assertTrue(result.getArrivalTime().isAfter(time));
        });

    }

//    @Test
//    void shouldHaveAbilityToChangeAtPiccGardens() {
//        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
//
//        assertTrue(interchangeRepository.isInterchange(PiccadillyGardens.from(stationRepository)));
//    }

    @Test
    void shouldFindStPetersToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        //journeyRequest.setDiag(true);

        List<Journey> results = calculator.calculateRouteAsList(StPetersSquare, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            assertEquals(1, result.getStages().size(), results.toString());
            assertEquals(TransportMode.Connect, result.getStages().getFirst().getMode());
        });

        results.forEach(result -> {
            assertFalse(result.getPath().contains(Piccadilly.from(stationRepository)));
        });
    }

    @Test
    void shouldFindDeansgateToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(Deansgate, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            assertEquals(2, result.getStages().size(), result.toString());
            assertEquals(TransportMode.Tram, result.getStages().getFirst().getMode());
            assertEquals(TransportMode.Connect, result.getStages().getLast().getMode());
        });

        results.forEach(result -> {
            assertFalse(result.getPath().contains(Piccadilly.from(stationRepository)));
        });
    }

    @Test
    void shouldCheckForExpectedRelationshipsAtPiccadilly() {

        Station piccadilly = Piccadilly.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransactionNeo4J txn = graphDatabase.beginTx()) {
            ImmutableGraphNode piccadillyNode = txn.findNode(piccadilly);
            ImmutableGraphNode stPetersSquareNode = txn.findNode(stPetersSquare);

            assertTrue(piccadillyNode.hasRelationship(GraphDirection.Incoming, DIVERSION));
            ImmutableGraphRelationship incomingDiversion = piccadillyNode.getSingleRelationship(txn, DIVERSION, GraphDirection.Incoming);
            assertEquals(STPETERS_PICC_WALK, incomingDiversion.getCost());
            assertEquals(stPetersSquareNode, incomingDiversion.getStartNode(txn));

            assertTrue(piccadillyNode.hasRelationship(GraphDirection.Outgoing, DIVERSION));
            ImmutableGraphRelationship outgoingDiversion = piccadillyNode.getSingleRelationship(txn, DIVERSION, GraphDirection.Outgoing);
            assertEquals(STPETERS_PICC_WALK, outgoingDiversion.getCost());
            assertEquals(stPetersSquareNode, outgoingDiversion.getEndNode(txn));
        }
    }

    @Test
    void shouldCheckForExpectedRelationshipsAtPiccadillyGardens() {

        Station piccadillyGardens = PiccadillyGardens.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransactionNeo4J txn = graphDatabase.beginTx()) {
            ImmutableGraphNode piccadillyGardensNode = txn.findNode(piccadillyGardens);
            ImmutableGraphNode stPetersSquareNode = txn.findNode(stPetersSquare);

            assertTrue(piccadillyGardensNode.hasRelationship(GraphDirection.Incoming, DIVERSION));
            ImmutableGraphRelationship incomingDiversion = piccadillyGardensNode.getSingleRelationship(txn, DIVERSION, GraphDirection.Incoming);
            assertEquals(STPETERS_PICC_GARDENS_WALK, incomingDiversion.getCost());
            assertEquals(stPetersSquareNode, incomingDiversion.getStartNode(txn));

            assertTrue(piccadillyGardensNode.hasRelationship(GraphDirection.Outgoing, DIVERSION));
            ImmutableGraphRelationship outgoingDiversion = piccadillyGardensNode.getSingleRelationship(txn, DIVERSION, GraphDirection.Outgoing);
            assertEquals(STPETERS_PICC_GARDENS_WALK, outgoingDiversion.getCost());
            assertEquals(stPetersSquareNode, outgoingDiversion.getEndNode(txn));
        }
    }

    @Test
    void shouldCheckForExpectedIncomingRelationshipsAtStPeters() {

        Station piccadilly = Piccadilly.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);
        Station piccGardens = PiccadillyGardens.from(stationRepository);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransactionNeo4J txn = graphDatabase.beginTx()) {
            ImmutableGraphNode stPetersSquareNode = txn.findNode(stPetersSquare);
            ImmutableGraphNode piccadillyNode = txn.findNode(piccadilly);
            ImmutableGraphNode piccGradensNode = txn.findNode(piccGardens);

            assertTrue(stPetersSquareNode.hasRelationship(GraphDirection.Incoming, DIVERSION));
            List<ImmutableGraphRelationship> incomingDiversions = stPetersSquareNode.
                    getRelationships(txn, GraphDirection.Incoming, DIVERSION).toList();

            assertEquals(2, incomingDiversions.size());

            Optional<ImmutableGraphRelationship> findFromPicc = incomingDiversions.stream().
                    filter(relationship -> relationship.getStartNode(txn).equals(piccadillyNode)).findFirst();

            assertTrue(findFromPicc.isPresent());
            ImmutableGraphRelationship fromPicc = findFromPicc.get();
            assertEquals(STPETERS_PICC_WALK, fromPicc.getCost());

            Optional<ImmutableGraphRelationship> findFromPiccGardens = incomingDiversions.stream().
                    filter(relationship -> relationship.getStartNode(txn).equals(piccGradensNode)).findFirst();

            assertTrue(findFromPiccGardens.isPresent());
            ImmutableGraphRelationship fromPiccGardens = findFromPiccGardens.get();
            assertEquals(STPETERS_PICC_GARDENS_WALK, fromPiccGardens.getCost());
        }
    }

    @Test
    void shouldCheckForExpectedOutgoingRelationshipsAtStPeters() {

        Station piccadilly = Piccadilly.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);
        Station piccGardens = PiccadillyGardens.from(stationRepository);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransactionNeo4J txn = graphDatabase.beginTx()) {
            ImmutableGraphNode stPetersSquareNode = txn.findNode(stPetersSquare);
            ImmutableGraphNode piccadillyNode = txn.findNode(piccadilly);
            ImmutableGraphNode piccGradensNode = txn.findNode(piccGardens);

            assertTrue(stPetersSquareNode.hasRelationship(GraphDirection.Outgoing, DIVERSION));
            List<ImmutableGraphRelationship> outgoingDiversions = stPetersSquareNode.
                    getRelationships(txn, GraphDirection.Outgoing, DIVERSION).toList();

            assertEquals(2, outgoingDiversions.size());

            Optional<ImmutableGraphRelationship> findTowardsPiccadilly = outgoingDiversions.stream().
                    filter(relationship -> relationship.getEndNode(txn).equals(piccadillyNode)).findFirst();

            assertTrue(findTowardsPiccadilly.isPresent());
            ImmutableGraphRelationship towardsPiccadilly = findTowardsPiccadilly.get();
            assertEquals(STPETERS_PICC_WALK, towardsPiccadilly.getCost());

            Optional<ImmutableGraphRelationship> findTowardsPiccGardens = outgoingDiversions.stream().
                    filter(relationship -> relationship.getEndNode(txn).equals(piccGradensNode)).findFirst();

            assertTrue(findTowardsPiccGardens.isPresent());
            ImmutableGraphRelationship towardsPiccGardens = findTowardsPiccGardens.get();
            assertEquals(STPETERS_PICC_GARDENS_WALK, towardsPiccGardens.getCost());

        }
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_central_with_temp_walks_trams.dot"), StPetersSquare.fake(), 100, true);
    }

    @NotNull
    private StationsWithDiversionRepository recreateRepository() {
        GraphFilter graphFilter = componentContainer.get(GraphFilter.class);
        ClosedStationsRepository closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
        StationsAndLinksGraphBuilder.Ready ready = componentContainer.get(StationsAndLinksGraphBuilder.Ready.class);
        Geography geography = componentContainer.get(Geography.class);
        StationsWithDiversion stationsWithDiversions = componentContainer.get(StationsWithDiversion.class);

        AddDiversionsForClosedGraphBuilder again = new AddDiversionsForClosedGraphBuilder(database, graphFilter, closedStationRepository,
                config, ready, stationsWithDiversions, geography, stationRepository);

        again.start();
        return stationsWithDiversions;
    }

    private static class SubgraphConfig extends IntegrationTramClosedStationsTestConfig {
        public SubgraphConfig(List<TemporaryStationsWalkIds> walks) {
            super(Collections.emptyList(), true, walks);
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }
    }
}
