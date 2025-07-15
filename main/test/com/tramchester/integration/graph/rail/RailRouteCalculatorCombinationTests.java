package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.Train;

@TrainTest
@Disabled("currently take too long")
class RailRouteCalculatorCombinationTests {

    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabaseNeo4J database;
    private RouteCalculationCombinations<Station> combinations;

    private final TramDate when = TestEnv.testDay();
    private MutableGraphTransaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.GreaterManchester);
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
        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkStationOpen(componentContainer) );
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveInterchangesToInterchanges() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(when, travelTime, false,
                10, Duration.ofHours(8), 1, getRequestedModes());

        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).interchangeToInterchange(Train);
        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, request, true);
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return EnumSet.noneOf(TransportMode.class);
    }

    @Test
    void shouldHaveEndsOfLinesToEndsOfLines() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(when, travelTime, false,
                10, Duration.ofHours(8), 1, getRequestedModes());

        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).endOfRoutesToEndOfRoutes(Train);
        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, request, true);
    }

}
