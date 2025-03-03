package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TemporaryStationWalksRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Connect;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@ExtendWith(ConfigParameterResolver.class)
@DataUpdateTest
@DualTest
@DisabledUntilDate(year = 2025, month = 3, day = 4)
public class RouteCalculatorForYorkStreetClosureTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig config;

    private RouteCalculatorTestFacade calculator;
    private MutableGraphTransaction txn;
    private Duration maxJourneyDuration;
    private int maxNumResults;
    private TramDate when;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        stationRepository = componentContainer.get(StationRepository.class);
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        maxNumResults = config.getMaxNumResults();

        when = UpcomingDates.YorkStreetWorks2025.getStartDate().plusDays(1);

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
            assertEquals(UpcomingDates.YorkStreetWorks2025, walk.getDateRange());
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

    private void validateExpectedConnections(List<Journey> results, Function<Journey, TransportStage<?,?>> getStage) {
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