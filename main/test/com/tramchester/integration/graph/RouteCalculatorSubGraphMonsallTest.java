package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Landslide2024TestCategory;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static java.lang.String.format;

@Landslide2024TestCategory
class RouteCalculatorSubGraphMonsallTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;
    private static TramRouteHelper tramRouteHelper;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();
    private MutableGraphTransaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();

        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphMonsallTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);

    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        graphFilter.addRoutes(tramRouteHelper.getId(KnownTramRoute.RochdaleShawandCromptonManchesterEastDidisbury));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTxMutable();
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);

    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithNotFindingDirectRouting() {

        // Can be direct or with a change depending on the timetable data
        validateNumberOfStages(TramStations.Monsall, TramStations.RochdaleRail, TramTime.of(8,5),
                when, 1);

        // direct
        validateNumberOfStages(TramStations.Monsall, TramStations.RochdaleRail, TramTime.of(8,10),
                when, 1);
    }

    @Landslide2024TestCategory
    @Test
    void shouldHaveEndToEnd() {
        validateNumberOfStages(TramStations.EastDidsbury, TramStations.Rochdale, TramTime.of(8,0), when, 1);
    }

    @Landslide2024TestCategory
    @Test
    void shouldHaveJourneysTerminationPointsToEndOfLine() {
        // many trams only run as far as Shaw
        validateNumberOfStages(TramStations.ShawAndCrompton, TramStations.Rochdale, TramTime.of(8,0), when, 1);
    }

    @Landslide2024TestCategory
    @Test
    void shouldHaveSimpleOneStopJourney() {
        validateNumberOfStages(TramStations.RochdaleRail, TramStations.Rochdale, TramTime.of(8,0), when, 1);
    }

    private void validateNumberOfStages(TramStations start, TramStations destination, TramTime time, TramDate date, int numStages) {
        long maxNumberOfJourneys = 1;
        JourneyRequest journeyRequest = new JourneyRequest(date, time,
                false, 3, Duration.ofMinutes(config.getMaxJourneyDuration()), maxNumberOfJourneys, TramsOnly);
        List<Journey> journeys = calculator.calculateRouteAsList(start, destination, journeyRequest);

        Assertions.assertFalse(journeys.isEmpty(), format("No Journeys from %s to %s found at %s on %s", start, destination, time.toString(), date));
        journeys.forEach(journey -> Assertions.assertEquals(numStages, journey.getStages().size()));
    }


    private static class SubgraphConfig extends IntegrationTramTestConfig {

        public SubgraphConfig() {
            super(Collections.emptyList(), Caching.Disabled);
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }
    }
}
