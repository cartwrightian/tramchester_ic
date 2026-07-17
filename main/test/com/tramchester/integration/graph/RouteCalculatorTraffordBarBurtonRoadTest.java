package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.DiagramCreator;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class RouteCalculatorTraffordBarBurtonRoadTest {
    private static ComponentContainer componentContainer;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();

    private static final List<IdFor<Station>> tramStations = Arrays.asList(
            TraffordBar.getId(),
            Firswood.getId(),
            Chorlton.getId(),
            StWerburghsRoad.getId(),
            Station.createId("9400ZZMAWIT"),
            BurtonRoad.getId()
    );

    private GraphTransaction txn;

    private TramDuration maxJourneyDuration;
    private int maxChanges;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();

        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorTraffordBarBurtonRoadTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());

        componentContainer.initialise();

    }

    private static void configureFilter(ConfigurableGraphFilter toConfigure, RouteRepository routeRepository) {
        tramStations.forEach(toConfigure::addStation);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        maxChanges = config.getMaxNumberChanges();

        maxJourneyDuration = TramDuration.ofMinutes(config.getMaxJourneyDuration());
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveChorltonToBurtonRoad() {
        List<Journey> results = validateAtLeastOneJourney(Chorlton, BurtonRoad, TramTime.of(9, 0), when);
        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
        });
    }

    @Test
    void shouldHaveTraffordBarToChorlton() {
        List<Journey> results = validateAtLeastOneJourney(TraffordBar, Chorlton, TramTime.of(9, 0), when);
        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
        });
    }

    @Test
    void shouldHaveTraffordBarToBurtonRoad() {

        Set<String> ids = Stream.of(Chorlton, Firswood).
                map(FakeStation::getId).
                map(IdFor::getGraphId).
                collect(Collectors.toSet());

        List<Journey> results = validateAtLeastOneJourney(TraffordBar, BurtonRoad, TramTime.of(9, 0), when);
        results.forEach(journey -> {
            assertEquals(2, journey.getStages().size());

            TransportStage<? extends Location<?>, ? extends Location<?>> stageOne = journey.getStages().getFirst();
            IdFor<?> stageOneLastStationId = stageOne.getLastStation().getId();
            assertTrue(ids.contains(stageOneLastStationId.getGraphId()), "Wrong id " + stageOneLastStationId);

            TransportStage<?, ?> stageTwo = journey.getStages().getLast();
            IdFor<?> stageTwoFirstStationId = stageTwo.getFirstStation().getId();
            assertTrue(ids.contains(stageTwoFirstStationId.getGraphId()), "Wrong id " + stageTwoFirstStationId);

        });
    }

    @Test
    void shouldHaveTraffordBarToFirswood() {
        List<Journey> results = validateAtLeastOneJourney(TraffordBar, Firswood, TramTime.of(9, 0), when);
        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
        });
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_traffordBar_BurtonRoad.dot"), MediaCityUK.fake(), 100, true);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super(IntegrationTramTestConfig.CurrentClosures);
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {

            ImmutableIdSet<Station> additionalInterchanges = AdditionalTramInterchanges.stations();

            final Set<TransportMode> groupStationModes = Collections.emptySet();

            TFGMGTFSSourceTestConfig gtfsSourceConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
                    Tram, additionalInterchanges, groupStationModes, IntegrationTramTestConfig.CurrentClosures,
                    TramDuration.ofMinutes(45), Collections.emptyList());

            return Collections.singletonList(gtfsSourceConfig);
        }

        @Override
        public Path getCacheFolder() {
            return TestEnv.CACHE_DIR.resolve("RouteCalculatorTraffordBarBurtonRoad");
        }
    }

    private List<Journey> validateAtLeastOneJourney(TramStations start, TramStations dest, TramTime time, TramDate date) {
        JourneyRequest journeyRequest = new JourneyRequest(date, time, false, maxChanges,
                maxJourneyDuration, 1, TramsOnly);
        journeyRequest.setDiag(false);
        List<Journey> results = calculator.calculateRouteAsList(start, dest, journeyRequest);
        assertFalse(results.isEmpty(), format("no journey from %s to %s at %s %s", start, dest, date, time));
        return results;
    }
}
