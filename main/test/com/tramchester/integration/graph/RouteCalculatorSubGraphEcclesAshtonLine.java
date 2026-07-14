package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.DiagramCreator;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.Summer2026Closures;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Summer2026Closures
class RouteCalculatorSubGraphEcclesLineSundayTest {
    private static ComponentContainer componentContainer;

    private static SubgraphConfig config;
    private static TramRouteHelper tramRouteHelper;

    private RouteCalculatorTestFacade calculator;
    private final static TramDate sunday = UpcomingDates.nextSunday();

    private GraphTransaction txn;
    private TramDuration maxJourneyDuration;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphEcclesLineSundayTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());

        componentContainer.initialise();

        tramRouteHelper = new TramRouteHelper(componentContainer);

    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        Route route = tramRouteHelper.getBlue(sunday);
        graphFilter.addRoute(route.getId());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);

        maxJourneyDuration = TramDuration.ofMinutes(config.getMaxJourneyDuration());
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithMediaCityToVelopark() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(8, 5), false,
                1, maxJourneyDuration, 2, TramsOnly);

        assertFalse(calculator.calculateRouteAsList(MediaCityUK, VeloPark, request).isEmpty());
    }

    @Test
    void shouldHaveEcclesCornbrook() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(9, 30), false,
                0, maxJourneyDuration, 2, TramsOnly);

        assertFalse(calculator.calculateRouteAsList(Eccles, Cornbrook, request).isEmpty());
    }

    @Test
    void shouldHaveBroadwayCornbrook() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(6, 50), false,
                0, maxJourneyDuration, 1, TramsOnly);

        assertFalse(calculator.calculateRouteAsList(Broadway, Cornbrook, request).isEmpty());
    }

    @Test
    void shouldHaveBroadwayHarbourCity() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(6, 50), false,
                0, maxJourneyDuration, 1, TramsOnly);

        request.setDiag(true);

        assertFalse(calculator.calculateRouteAsList(Broadway, HarbourCity, request).isEmpty());
    }

    @Test
    void shouldHaveBroadwayMediaCity() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(9, 30), false,
                0, maxJourneyDuration, 1, TramsOnly);

        assertFalse(calculator.calculateRouteAsList(Broadway, MediaCityUK, request).isEmpty());
    }

    @Test
    void shouldHaveMediaCityHarbourCity() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(9, 30), false,
                0, maxJourneyDuration, 1, TramsOnly);

        assertFalse(calculator.calculateRouteAsList(MediaCityUK, HarbourCity, request).isEmpty());
    }

    @Test
    void shouldHaveMediaCityCornbrook() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(9, 30), false,
                0, maxJourneyDuration, 2, TramsOnly);

        //request.setDiag(true);

        assertFalse(calculator.calculateRouteAsList(MediaCityUK, Cornbrook, request).isEmpty());
    }

    @Test
    void shouldHaveCornbrookEccles() {
        JourneyRequest request = new JourneyRequest(sunday, TramTime.of(9, 30), false,
                0, maxJourneyDuration, 2, TramsOnly);

//        request.setDiag(true);

        assertFalse(calculator.calculateRouteAsList(Cornbrook, Eccles, request).isEmpty());
    }


    @Disabled
    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        List<Station> starts = Arrays.asList(VeloPark.fake(), Etihad.fake());
        creator.create(Path.of("subgraph_eccles_ashton_velo.dot"),starts, 2, true);
    }

    @Disabled
    @Test
    void produceDiagramOfGraphSubsetEtihad() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        List<Station> starts = Collections.singletonList(Etihad.fake());
        creator.create(Path.of("subgraph_eccles_ashton_etihad.dot"),starts, 1, false);
    }

    @Disabled
    @Test
    void produceDiagramOfGraphSubsetMediaCity() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        List<Station> starts = Arrays.asList(MediaCityUK.fake(), HarbourCity.fake(),
                Broadway.fake());
        creator.create(Path.of("subgraph_eccles_ashton_mediaCity.dot"),starts, 2, true);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            // TODO no closures, but is this correct?
            super(Collections.emptyList());
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }

        @Override
        public Path getCacheFolder() {
            return TestEnv.CACHE_DIR.resolve("RouteCalculatorSubGraphEcclesLine");
        }
    }

}
