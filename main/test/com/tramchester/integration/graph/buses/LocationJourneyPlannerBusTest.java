package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrinchamInterchange;
import static com.tramchester.testSupport.reference.KnownLocations.nearKnutsfordBusStation;
import static org.junit.jupiter.api.Assertions.assertFalse;

@BusTest
class LocationJourneyPlannerBusTest {
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;
    private Duration maxDuration;

    private final TramDate nextMonday = TestEnv.nextMonday();
    private MutableGraphTransaction txn;
    private LocationJourneyPlannerTestFacade planner;
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabaseNeo4J.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        planner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        maxDuration = Duration.ofMinutes(testConfig.getMaxJourneyDuration());
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveSimpleWalkAndBus() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest journeyRequest = new JourneyRequest(nextMonday, travelTime, false, 3,
                maxDuration, 1, getRequestedModes());

        StationLocalityGroup end = KnownLocality.Stockport.from(stationGroupsRepository);

        Set<Journey> results = planner.quickestRouteForLocation(nearAltrinchamInterchange, end, journeyRequest, 5);

        assertFalse(results.isEmpty());
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return BusesOnly;
    }

    @Test
    void shouldHaveSimpleBusAndWalk() {

        StationLocalityGroup stockportBusStation = KnownLocality.Stockport.from(stationGroupsRepository);

        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest journeyRequest = new JourneyRequest(nextMonday, travelTime, false, 3,
                maxDuration, 1, getRequestedModes());

        Set<Journey> results = planner.quickestRouteForLocation(stockportBusStation, nearAltrinchamInterchange,
                journeyRequest, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldFindAltyToKnutford() {

        StationLocalityGroup alty = KnownLocality.Altrincham.from(stationGroupsRepository);

        TramTime travelTime = TramTime.of(10, 30);

        JourneyRequest request = new JourneyRequest(nextMonday, travelTime, false, 3,
                maxDuration, 1, getRequestedModes());
        Set<Journey> journeys =  planner.quickestRouteForLocation(alty, nearKnutsfordBusStation, request, 5);

        assertFalse(journeys.isEmpty());
    }



}
