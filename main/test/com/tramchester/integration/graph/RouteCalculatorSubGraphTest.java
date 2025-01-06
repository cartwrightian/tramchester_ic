package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.KnownLocations.nearStPetersSquare;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteCalculatorSubGraphTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();
    private static final List<TramStations> stations = Arrays.asList(
            Cornbrook,
            StPetersSquare,
            Deansgate,
            Pomona);
    private MutableGraphTransaction txn;
    private TramTime tramTime;
    private Duration maxJourneyDuration;
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();

        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        stations.forEach(station -> graphFilter.addStation(station.getId()));
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

        tramTime = TramTime.of(8, 0);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());

        modes = TramsOnly;
    }

    @AfterEach
    void onceAfterEveryTest() {
        txn.close();
    }

    @Test
    void reproduceIssueEdgePerTrip() {

        validateAtLeastOneJourney(StPetersSquare, TramStations.Deansgate,
                new JourneyRequest(when, tramTime, false, 5, maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(Cornbrook, Pomona,
                new JourneyRequest(when, TramTime.of(19,51).plusMinutes(6), false, 5,
                        maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(TramStations.Deansgate, Cornbrook,
                new JourneyRequest(when, TramTime.of(19,51).plusMinutes(3), false, 5,
                        maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(TramStations.Deansgate, Pomona,
                new JourneyRequest(when, TramTime.of(19,51).plusMinutes(3), false, 5,
                        maxJourneyDuration, 1, modes));

        validateAtLeastOneJourney(StPetersSquare, Pomona, new JourneyRequest(when, tramTime,
                false, 5, maxJourneyDuration, 1, modes));
        validateAtLeastOneJourney(StPetersSquare, Pomona, new JourneyRequest(when, tramTime,
                false, 5, maxJourneyDuration, 1, modes));
    }

    @Test
    void shouldHandleCrossingMidnightDirect() {
        validateAtLeastOneJourney(Cornbrook, StPetersSquare, new JourneyRequest(when, tramTime, false, 5,
                maxJourneyDuration, 1, modes));
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldHaveJourneysBetweenAllStations() {
        for (TramStations start: stations) {
            for (TramStations destination: stations) {
                if (!start.equals(destination)) {
                    JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 5, maxJourneyDuration,
                            1, modes);
                    validateAtLeastOneJourney(start, destination, journeyRequest);
                }
            }
        }
    }

    @Test
    void shouldHaveWalkAtEnd() {

        LocationJourneyPlanner planner = componentContainer.get(LocationJourneyPlanner.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        LocationJourneyPlannerTestFacade testFacade = new LocationJourneyPlannerTestFacade(planner, stationRepository, txn);

        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 3,
                maxJourneyDuration,1, modes);
        //journeyRequest.setDiag(true);
        final Station station = stationRepository.getStationById(Pomona.getId());
        Set<Journey> results = testFacade.quickestRouteForLocation(station, nearStPetersSquare, journeyRequest, 4);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleOneStopJourney() {
        List<Journey> results = getJourneys(Cornbrook, Pomona, when, 1);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleOneStopJourneyLateNight() {
        // last tram now earlier
        TramTime time = TramTime.of(23,40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 3,
                maxJourneyDuration, 1, modes);
//        journeyRequest.setDiag(true);
        List<Journey> results = calculator.calculateRouteAsList(Cornbrook, Pomona, journeyRequest);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleOneStopJourneyAtWeekend() {
        List<Journey> results = getJourneys(Cornbrook, Pomona, UpcomingDates.nextSaturday(), 1);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleOneStopJourneyBetweenInterchanges() {
        List<Journey> results = getJourneys(StPetersSquare, TramStations.Deansgate, when, 1);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveSimpleJourney() {
        List<Journey> results = getJourneys(StPetersSquare, Cornbrook, when, 1);
        assertFalse(results.isEmpty());
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        //DiagramCreator creator = new DiagramCreator(database);
        creator.create(Path.of("subgraph_trams.dot"), Cornbrook.fake(), 100, false);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            // TODO no closures, but is this valid??
            super(Collections.emptyList());
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }


        @Override
        public Path getCacheFolder() {
            return TestEnv.CACHE_DIR.resolve("RouteCalculatorSubGraphTest");
        }
    }

    @NotNull
    private List<Journey> getJourneys(TramStations start, TramStations destination, TramDate when, long maxNumberJourneys) {
        JourneyRequest journeyRequest = new JourneyRequest(when, tramTime, false, 3,
                maxJourneyDuration, maxNumberJourneys, modes);
        return calculator.calculateRouteAsList(start,destination, journeyRequest);
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, JourneyRequest journeyRequest) {
        // TODO Use find any on stream instead
        final List<Journey> results = calculator.calculateRouteAsList(start, dest, journeyRequest);
        assertFalse(results.isEmpty());
    }
}
