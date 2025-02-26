package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.TemporaryStationWalksRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Connect;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
@Disabled("WIP")
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

        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 2,
                maxJourneyDuration, maxNumResults, EnumSet.of(Tram));

        List<Journey> results = calculator.calculateRouteAsList(StPetersSquare, Piccadilly, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.getStages().toString());
            assertEquals(Connect, journey.getStages().getFirst().getMode());
            //assertEquals(Tram, journey.getStages().getLast().getMode());
        });
    }

    @Test
    void shouldFindWalkFromStPetersToPiccGardensDuringYorkStreetWork() {

        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0,
                maxJourneyDuration, maxNumResults, EnumSet.of(Tram));

        List<Journey> results = calculator.calculateRouteAsList(StPetersSquare, PiccadillyGardens, journeyRequest);

        assertFalse(results.isEmpty());

        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.getStages().toString());
            assertEquals(Connect, journey.getStages().getFirst().getMode());
        });
    }


}