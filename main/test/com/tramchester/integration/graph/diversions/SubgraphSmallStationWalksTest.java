package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.TemporaryStationsWalkConfigForTest;
import com.tramchester.integration.testSupport.tram.CentralStationsSubGraph;
import com.tramchester.integration.testSupport.tram.IntegrationTramStationWalksTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.PiccGardensWorkSummer2025;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.Connect;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@PiccGardensWorkSummer2025
class SubgraphSmallStationWalksTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static IntegrationTramStationWalksTestConfig config;

    private final static TramDate when = TestEnv.testDay();

    private RouteCalculatorTestFacade calculator;
    private ImmutableGraphTransactionNeo4J txn;
    private Duration maxJourneyDuration;
    private int maxChanges;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {

        StationIdPair stationIdPair = new StationIdPair(Piccadilly.getId(), PiccadillyGardens.getId());

        TemporaryStationsWalkIds temporaryStationsWalkA = new TemporaryStationsWalkConfigForTest(stationIdPair,
                DateRange.of(when.minusWeeks(1), when.plusWeeks(1)));

        config = new SubgraphConfig(List.of(temporaryStationsWalkA));
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

        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        maxJourneyDuration = Duration.ofMinutes(30);
        maxChanges = 2;
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return TramsOnly;
    }

    @Test
    void shouldHaveJourneyFromPiccGardensToVictoria() {
        JourneyRequest journeyRequest = new JourneyRequest(when.plusDays(1), TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(PiccadillyGardens, Victoria, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");
    }

    @Test
    void shouldFindRouteUsingWalkCornbrookToPicc() {

        // TODO likely need the closures here as well, otherwise chance will end up with Tram journey without walks

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                2, maxJourneyDuration, 1, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(Cornbrook, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
           assertEquals(2, result.getStages().size(), result.toString());
           assertEquals(Connect, result.getStages().get(1).getMode());
        });
    }

    @Disabled("Tram is faster")
    @Test
    void shouldFindRouteUsingWalkPiccToCornbrook() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(Piccadilly, Cornbrook, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            assertEquals(Connect, result.getStages().get(0).getMode(), "wrong mode? " + result);
        });
    }

    @Disabled("Tram is faster")
    @Test
    void shouldFindRouteUsingWalkPiccToPiccGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(8,0), false,
                maxChanges, maxJourneyDuration, 1, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(Piccadilly, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty(), "no journeys");

        results.forEach(result -> {
            assertEquals(1, result.getStages().size(), result.toString());
            assertEquals(Connect, result.getStages().get(0).getMode(), "wrong mode? " + result);
        });
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_central_with_station_walks_trams.dot"), StPetersSquare.fake(), 100, true);
    }

    private static class SubgraphConfig extends IntegrationTramStationWalksTestConfig {
        public SubgraphConfig(List<TemporaryStationsWalkIds> temporaryWalks) {
            super(temporaryWalks);
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }
    }
}
