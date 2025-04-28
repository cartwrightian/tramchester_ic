package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.AddDiversionsForClosedGraphBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.StationsWithDiversion;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.closures.StationClosuresListForTest;
import com.tramchester.integration.testSupport.tram.CentralStationsSubGraph;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.graphbuild.GraphLabel.PLATFORM;
import static com.tramchester.graph.graphbuild.GraphLabel.ROUTE_STATION;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class SubgraphSmallClosedStationsDiversionsTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramClosedStationsTestConfig config;

    private final static TramDate when = TestEnv.testDay();

    private final static List<StationClosures> closedStations = List.of(
            new StationClosuresListForTest(PiccadillyGardens, new DateRange(when.plusWeeks(1), when.plusWeeks(2)), false));

    private RouteCalculatorTestFacade calculator;
    private StationRepository stationRepository;
    private ImmutableGraphTransaction txn;
    private Duration maxJourneyDuration;
    private int maxChanges;
    private StationsWithDiversionRepository diversionRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig(closedStations);
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
        maxJourneyDuration = Duration.ofMinutes(30);
        maxChanges = 2;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldValidateRangeIsCorrectForDiversions() {
        StationLocations stationLocations = componentContainer.get(StationLocations.class);

        MarginInMeters range = config.getWalkingDistanceRange();

        Station piccGardens = PiccadillyGardens.from(stationRepository);

        Set<Station> withinRange = stationLocations.nearestStationsUnsorted(piccGardens, range).collect(Collectors.toSet());

        assertEquals(9, withinRange.size(), HasId.asIds(withinRange));

        // ignore self
        assertTrue(withinRange.contains(MarketStreet.from(stationRepository)));
        assertTrue(withinRange.contains(StPetersSquare.from(stationRepository)));
        assertTrue(withinRange.contains(Piccadilly.from(stationRepository)));
        assertTrue(withinRange.contains(Shudehill.from(stationRepository)));
        assertTrue(withinRange.contains(ExchangeSquare.from(stationRepository)));
    }

    @Test
    void shouldHaveTheDiversionsInTheRepository() {
        assertTrue(diversionRepository.hasDiversions(MarketStreet.from(stationRepository)));
        assertTrue(diversionRepository.hasDiversions(StPetersSquare.from(stationRepository)));
        assertTrue(diversionRepository.hasDiversions(Piccadilly.from(stationRepository)));
        assertTrue(diversionRepository.hasDiversions(Shudehill.from(stationRepository)));
        assertTrue(diversionRepository.hasDiversions(ExchangeSquare.from(stationRepository)));

        // try again but via load from DB, instead of during population
        StationsWithDiversionRepository again = recreateRepository();

        assertTrue(again.hasDiversions(MarketStreet.from(stationRepository)));
        assertTrue(again.hasDiversions(StPetersSquare.from(stationRepository)));
        assertTrue(again.hasDiversions(Piccadilly.from(stationRepository)));
        assertTrue(again.hasDiversions(Shudehill.from(stationRepository)));
        assertTrue(again.hasDiversions(ExchangeSquare.from(stationRepository)));
    }

    @Test
    void shouldHaveCorrectDateRangesForDiversion() {
        DateRange expected = DateRange.of(when.plusWeeks(1), when.plusWeeks(2));

        hasDateRange(MarketStreet.from(stationRepository), expected, diversionRepository);
        hasDateRange(StPetersSquare.from(stationRepository), expected, diversionRepository);
        hasDateRange(Piccadilly.from(stationRepository), expected, diversionRepository);
        hasDateRange(Shudehill.from(stationRepository), expected, diversionRepository);
        hasDateRange(ExchangeSquare.from(stationRepository), expected, diversionRepository);

        // try again but via load from DB, instead of during population
        StationsWithDiversionRepository again = recreateRepository();
        hasDateRange(MarketStreet.from(stationRepository), expected, again);
        hasDateRange(StPetersSquare.from(stationRepository), expected, again);
        hasDateRange(Piccadilly.from(stationRepository), expected, again);
        hasDateRange(Shudehill.from(stationRepository), expected, again);
        hasDateRange(ExchangeSquare.from(stationRepository), expected, again);

    }

    private void hasDateRange(Station station, DateRange expected, StationsWithDiversionRepository repository) {
        final Set<DateTimeRange> ranges = repository.getDateTimeRangesFor(station);

        Set<DateRange> dateRanges = ranges.stream().map(DateTimeRange::getDateRange).collect(Collectors.toSet());
        assertTrue(dateRanges.contains(expected));

        ranges.forEach(range -> assertTrue(range.getTimeRange().allDay(), "Expected all day time range for " + range));
    }

    @Test
    void shouldHaveExpectedRouteToRouteCostsForClosedStations() {

        Location<?> start = StPetersSquare.from(stationRepository);
        Location<?> destination = Piccadilly.from(stationRepository);

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
    void shouldHaveExpectedNeighboursForClosedPiccadillyGardens() {
        TramDate date = when.plusWeeks(1).plusDays(1);
        ClosedStationsRepository closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);

        Station piccGardens = PiccadillyGardens.from(stationRepository);
        assertTrue(closedStationsRepository.isClosed(piccGardens, date));
        ClosedStation closed = closedStationsRepository.getClosedStation(piccGardens, date, TimeRange.AllDay());

        Set<Station> nearby = closed.getDiversionAroundClosure();
        assertEquals(7, nearby.size(), HasId.asIds(nearby));
        assertTrue(nearby.contains(Piccadilly.from(stationRepository)));
        assertTrue(nearby.contains(MarketStreet.from(stationRepository)));
        assertTrue(nearby.contains(StPetersSquare.from(stationRepository)));
        assertTrue(nearby.contains(Shudehill.from(stationRepository)));
        assertTrue(nearby.contains(ExchangeSquare.from(stationRepository)));
    }

    @Test
    void shouldHaveJourneyFromPiccGardensToPiccadilly() {
        JourneyRequest journeyRequest = new JourneyRequest(when.plusDays(1), TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(PiccadillyGardens, Victoria, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramCornbrookToPicc() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(Cornbrook, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramPiccToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(Piccadilly, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramVicToPicc() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(Victoria, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteAroundCloseBackOnToTramPiccToVic() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(Piccadilly, Victoria, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteWhenFromStationWithDiversionToOtherDiversionStation() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);
        List<Journey> results = calculator.calculateRouteAsList(ExchangeSquare, Deansgate, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindPiccadillyToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when.plusDays(1), TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);
        List<Journey> results = calculator.calculateRouteAsList(Piccadilly, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindStPetersToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);
        List<Journey> results = calculator.calculateRouteAsList(StPetersSquare, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindDeansgateToPiccadillyGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(Deansgate, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldCheckForExpectedInboundRelationships() {
        List<GraphRelationshipId> foundRelationshipIds = new ArrayList<>();

        Station exchange = ExchangeSquare.from(stationRepository);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            exchange.getPlatforms().forEach(platform -> {
                GraphNode node = txn.findNode(platform);
                Stream<ImmutableGraphRelationship> iterable = node.getRelationships(txn, Direction.INCOMING, TransportRelationshipTypes.DIVERSION_DEPART);

                iterable.forEach(relationship -> foundRelationshipIds.add(relationship.getId()));
            });

        }

        assertFalse(foundRelationshipIds.isEmpty());

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            GraphRelationship relationship = txn.getRelationshipById(foundRelationshipIds.get(0));
            GraphNode from = relationship.getStartNode(txn);
            assertTrue(from.hasLabel(ROUTE_STATION), from.getAllProperties().toString());
            GraphNode to = relationship.getEndNode(txn);
            assertTrue(to.hasLabel(PLATFORM));
        }

    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_central_with_closure_trams.dot"), StPetersSquare.fake(), 100, true);
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
        public SubgraphConfig(List<StationClosures> closures) {
            super(closures, true, Collections.emptyList());
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }
    }
}
