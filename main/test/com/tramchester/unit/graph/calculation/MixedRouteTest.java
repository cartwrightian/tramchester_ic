package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.MixedTransportTestDataFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.MixedTransportTestDataFactory.MixedTransportTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class MixedRouteTest {

    private static MixedTransportTestDataFactory.MixedTransportTestData transportData;
    private static RouteCalculator calculator;
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SimpleMixedRouteGraphConfig config;

    private TramDate queryDate;
    private TramTime queryTime;
    private ImmutableGraphTransaction txn;
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleMixedRouteGraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(MixedTransportTestDataFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        transportData = (MixedTransportTestDataFactory.MixedTransportTestData) componentContainer.get(TransportData.class);
        database = componentContainer.get(GraphDatabaseNeo4J.class);
        calculator = componentContainer.get(RouteCalculator.class);
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx();

        queryDate = TramDate.of(2014,6,30);
        queryTime = TramTime.of(7, 57);

        modes = config.getTransportModes();
    }

    @AfterEach
    void afterEachTestRuns()
    {
        txn.close();
    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, modes);
    }

    @Test
    void shouldTestSimpleJourneyIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getSecond(), journeyRequest, () -> true).
                collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLastForOneStage(journeys, FIRST_STATION, SECOND_STATION, 0, queryTime);
    }

    @Test
    void shouldTestMultiStopJourneyIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(), transportData.getLast(),
                journeyRequest, () -> true).collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLastForOneStage(journeys, FIRST_STATION, LAST_STATION, 2, queryTime);
    }

    @Test
    void shouldTestMultiStopJourneyFerryIsPossible() {
        ///
        // Note relies on multi-mode stations automatically being seen as interchanges
        // Change at Interchange ONLY is enabled in config below
        //
        assertTrue(config.getChangeAtInterchangeOnly(),"valid precondition");
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFourthStation(), journeyRequest, () -> true).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size(), journey.toString());

            TransportStage<?,?> first = stages.get(0);
            assertEquals(Station.createId(FIRST_STATION), first.getFirstStation().getId(), journey.toString());

            TransportStage<?,?> last = stages.get(1);
            assertEquals(Station.createId(STATION_FOUR), last.getLastStation().getId(), journey.toString());
        });
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Assertions.assertAll(() -> creator.create(Path.of("mixed_test_network.dot"), transportData.getFirst(),
                Integer.MAX_VALUE, false));
    }

    private static void assertFirstAndLastForOneStage(Set<Journey> journeys, String firstStation, String secondStation,
                                                      int passedStops, TramTime queryTime) {
        Journey journey = (Journey)journeys.toArray()[0]; // TODO YUCK!
        List<TransportStage<?,?>> stages = journey.getStages();

        TransportStage<?,?> vehicleStage = stages.getFirst();
        assertEquals(Station.createId(firstStation), vehicleStage.getFirstStation().getId());
        assertEquals(Station.createId(secondStation), vehicleStage.getLastStation().getId());
        assertEquals(passedStops,  vehicleStage.getPassedStopsCount());
        Assertions.assertFalse(vehicleStage.hasBoardingPlatform());

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        assertTrue(departTime.isAfter(queryTime));

        assertFalse(vehicleStage.getDuration().isNegative());
        assertFalse(vehicleStage.getDuration().isZero());
    }

    private static class SimpleMixedRouteGraphConfig extends IntegrationTestConfig {

        public SimpleMixedRouteGraphConfig() {
            super(TestGroupType.unit);
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            Set<GTFSTransportationType> modes = new HashSet<>(
                    Arrays.asList(GTFSTransportationType.bus, GTFSTransportationType.tram, GTFSTransportationType.ferry));

            EnumSet<TransportMode> modesWithPlatforms = EnumSet.of(TransportMode.Tram);

            TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig(
                    modes, modesWithPlatforms,
                    IdSet.emptySet(), Collections.emptySet(), Collections.emptyList(), Duration.ofMinutes(13), Collections.emptyList());
            return Collections.singletonList(tfgmTestDataSourceConfig);
        }

        @Override
        public int getNumberQueries() { return 1; }

        @Override
        public int getQueryInterval() {
            return 6;
        }

        @Override
        public Path getCacheFolder() {
            return TestEnv.CACHE_DIR.resolve("mixedRouteTest");
        }
    }
}
