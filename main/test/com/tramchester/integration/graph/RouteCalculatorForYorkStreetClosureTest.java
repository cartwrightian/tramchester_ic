package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.TemporaryStationsWalkConfigForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TemporaryStationWalksRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Connect;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@DataUpdateTest
@Disabled("WIP")
public class RouteCalculatorForYorkStreetClosureTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabaseNeo4J database;
    private static TramchesterConfig config;

    private RouteCalculatorTestFacade calculator;
    private ImmutableGraphTransaction txn;
    private Duration maxJourneyDuration;
    private int maxNumResults;
    private TramDate when;
    private StationRepository stationRepository;

    public static final DateRange YorkStreetWorks2025 = DateRange.of(TramDate.of(2025,3,1),
        TramDate.of(2025, 3, 16));

    private static final TemporaryStationsWalkIds StPetersToPiccGardens = new TemporaryStationsWalkConfigForTest(
            StationIdPair.of(StPetersSquare, PiccadillyGardens),
            YorkStreetWorks2025);

    private static final TemporaryStationsWalkIds StPetersToPicc = new TemporaryStationsWalkConfigForTest(
            StationIdPair.of(TramStations.StPetersSquare, TramStations.Piccadilly),
            YorkStreetWorks2025);

    public static final List<TemporaryStationsWalkIds> YorkStreetClosureWalks = Arrays.asList(StPetersToPiccGardens, StPetersToPicc);


    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig(Collections.emptyList(), IntegrationTramTestConfig.Caching.Disabled,
                YorkStreetClosureWalks);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabaseNeo4J.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        maxNumResults = config.getMaxNumResults();

        when = YorkStreetWorks2025.getStartDate().plusDays(1);

    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveExpectedWalks() {
        TemporaryStationWalksRepository repository = componentContainer.get(TemporaryStationWalksRepository.class);

        Set<TemporaryStationWalk> walks = repository.getTemporaryWalksFor(DataSourceID.tfgm);

        assertEquals(2, walks.size());

        walks.forEach(walk -> {
            assertEquals(YorkStreetWorks2025, walk.getDateRange());
        });

        Set<StationIdPair> ids = walks.stream().map(walk -> walk.getStationPair().getStationIds()).collect(Collectors.toSet());

        assertTrue(ids.contains(StationIdPair.of(StPetersSquare, Piccadilly)));
        assertTrue(ids.contains(StationIdPair.of(StPetersSquare, PiccadillyGardens)));

    }

    @Test
    void shouldFindWalkFromStPetersToPiccDuringYorkStreetWork() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,0), false, 2,
                maxJourneyDuration, maxNumResults, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(StPetersSquare, Piccadilly, journeyRequest);

        validateExpectedConnections(results, journey -> journey.getStages().getFirst());
    }

    @Test
    void shouldFindWalkFromStPetersToPiccGardensDuringYorkStreetWork() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,0), false, 0,
                maxJourneyDuration, maxNumResults, EnumSet.of(Tram));

        List<Journey> results = calculator.calculateRouteAsList(StPetersSquare, PiccadillyGardens, journeyRequest);

        validateExpectedConnections(results, journey -> journey.getStages().getFirst());
    }

    @Test
    void shouldFindWalkFromPiccToStPetersToDuringYorkStreetWork() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,0), false, 2,
                maxJourneyDuration, maxNumResults, TramsOnly);

        List<Journey> results = calculator.calculateRouteAsList(Piccadilly, StPetersSquare, journeyRequest);

        validateExpectedConnections(results, journey -> journey.getStages().getLast());
    }

    @Test
    void shouldFindWalkFromPiccGardensToStPetersDuringYorkStreetWork() {

        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,0), false, 0,
                maxJourneyDuration, maxNumResults, EnumSet.of(Tram));

        List<Journey> results = calculator.calculateRouteAsList(PiccadillyGardens, StPetersSquare, journeyRequest);

        validateExpectedConnections(results, journey -> journey.getStages().getLast());
    }

    @Test
    void shouldFindDeansgateToPiccGardens() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,0), false, 2,
                maxJourneyDuration, maxNumResults, EnumSet.of(Tram));

        List<Journey> results = calculator.calculateRouteAsList(Deansgate, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());

        validateExpectedConnections(results, journey -> journey.getStages().getLast());
    }

    private void validateExpectedConnections(final List<Journey> results, Function<Journey, TransportStage<?,?>> getStage) {
        assertFalse(results.isEmpty());

        Location<?> marketStreet = MarketStreet.from(stationRepository);
        List<Journey> incorrectPath = results.stream().
                filter(journey -> journey.getPath().contains(marketStreet)).
                toList();
        assertTrue(incorrectPath.isEmpty(), incorrectPath.toString());

        List<Journey> incorrectStages = results.stream().
                filter(journey -> journey.getStages().size()>2).
                toList();
        assertTrue(incorrectStages.isEmpty(), incorrectStages.toString());

        List<Journey> incorrectConnect = results.stream().
                filter(journey -> !getStage.apply(journey).getMode().equals(Connect)).
                toList();
        assertTrue(incorrectConnect.isEmpty(), incorrectConnect.toString());
    }


}