package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.collections.RequestStopStream;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.StationBoxFactory;
import com.tramchester.geo.StationLocations;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculatorForBoxes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static org.junit.jupiter.api.Assertions.*;

class RouteCalculatorForBoundingBoxTest {
    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;

    private RouteCalculatorForBoxes calculator;
    private final TramDate when = TestEnv.testDay();
    private MutableGraphTransaction txn;
    private StationLocations stationLocations;
    private StationRepository stationRepository;
    private StationBoxFactory stationBoxFactory;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
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
        calculator = componentContainer.get(RouteCalculatorForBoxes.class);
        stationLocations = componentContainer.get(StationLocations.class);
        stationRepository = componentContainer.get(StationRepository.class);
        stationBoxFactory = componentContainer.get(StationBoxFactory.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveExpectedStationInBoxes() {
        BoundingBox bounds = stationLocations.getActiveStationBounds();
        int gridSize = (bounds.getMaxNorthings() - bounds.getMinNorthings()) / 100;

        final List<StationsBoxSimpleGrid> boxes = stationBoxFactory.getStationBoxes(gridSize, when);

        Set<Station> allStations = stationRepository.getStations();

        Set<Station> missing = allStations.stream().
                filter(station -> boxes.stream().noneMatch(box -> box.getStations().contains(station))).
                collect(Collectors.toSet());

        // checking issues causing problems, closed stations where diversions in place turn up in search space but
        // cannot be found by BreadthFirstBranchSelectorForGridSearch inside of one the station boxes
        assertTrue(missing.isEmpty(), HasId.asIds(missing));
    }

    @Test
    void shouldFindJourneysForBoundedBoxStations() throws InterruptedException {
        BoundingBox bounds = stationLocations.getActiveStationBounds();
        int gridSize = (bounds.getMaxNorthings()-bounds.getMinNorthings()) / 100;

        final List<StationsBoxSimpleGrid> boxes = stationBoxFactory.getStationBoxes(gridSize, when);

        long maxNumberOfJourneys = 3;
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(9,30),
                false, 3, Duration.ofMinutes(testConfig.getMaxJourneyDuration()), maxNumberOfJourneys,
                TramsOnly);

        Station destination = StPetersSquare.from(stationRepository);

        Optional<StationsBoxSimpleGrid> findDestBox = boxes.stream().filter(box -> box.getStations().contains(destination)).findFirst();
        assertTrue(findDestBox.isPresent());

        StationsBoxSimpleGrid destinationBox = findDestBox.get();

        // repro issue happening during closures summer 2024


        RequestStopStream<JourneysForBox> result = calculator.calculateRoutes(destinationBox, journeyRequest, boxes);

        Stream<JourneysForBox> stream = result.getStream();

        assertNotNull(stream);

        List<JourneysForBox> groupedJourneys = stream.toList();

        assertFalse(groupedJourneys.isEmpty());

        List<JourneysForBox> missed = groupedJourneys.stream().filter(group -> group.getJourneys().isEmpty()).toList();

        assertEquals(1, missed.size(), missed.toString()); // when start and dest match

        groupedJourneys.forEach(group -> group.getJourneys().forEach(journey -> {
            assertFalse(journey.getStages().isEmpty()); // catch case where starting point is dest
        } ));
    }

}
