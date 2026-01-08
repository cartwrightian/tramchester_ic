package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.TramRouteCalculator;
import com.tramchester.graph.search.inMemory.FindPathsForJourney;
import com.tramchester.graph.search.inMemory.ShortestPath;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.graph.search.LocationJourneyPlanner;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.KnownLocations.nearWythenshaweHosp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteCalculatorWithTestRouteInMemoryTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private Geography geography;

    private MutableGraphTransaction txn;
    private LocationJourneyPlannerTestFacade locationJourneyPlanner;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGroupedGraphConfig(GraphDBType.InMemory);
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTxMutable();

        // force DB build
        StagedTransportGraphBuilder transportGraphBuilder = componentContainer.get(StagedTransportGraphBuilder.class);
        transportGraphBuilder.getReady();

        config = componentContainer.get(TramchesterConfig.class);

        geography = componentContainer.get(Geography.class);

        StationRepository stationRepo = componentContainer.get(StationRepository.class);
        locationJourneyPlanner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class),
                stationRepo, txn);
    }

    @AfterEach
    void afterEachTestRuns()
    {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldHaveRouteCostCalculationAsExpected() throws InvalidDurationException {
        TramDate queryDate = TramTransportDataForTestFactory.startDate;

        RouteCostCalculator costCalculator = componentContainer.get(RouteCostCalculator.class);
        assertMinutesEquals(41, costCalculator.getAverageCostBetween(txn,
                transportData.getFirst(), transportData.getLast(), queryDate, EnumSet.of(Tram)));

//        assertEquals(-1, costCalculator.getAverageCostBetween(txn, transportData.getLast(), transportData.getFirst(), queryDate));
    }

    @Test
    void shouldFindShortestPaths() {

        Station begin = transportData.getFirst();
        Station dest = transportData.getLast();

        GraphNode beginNode = txn.findNode(begin);
        GraphNode destNode = txn.findNode(dest);

        ShortestPath shortestPath = new ShortestPath(txn, beginNode);

        final FindPathsForJourney.GraphRelationshipFilter filter = relationship ->
                RouteCostCalculator.costApproxTypes.contains(relationship.getType());
        final TramDuration result = shortestPath.findShortestPathsTo(destNode, filter);

        assertEquals(TramDuration.ofMinutes(41), result);

    }

    @Test
    void shouldDoInMemoryRouteCalcSimple() {
        TramRouteCalculator tramRouteCalculator = componentContainer.get(TramRouteCalculator.class);

        Station begin = transportData.getFirst();
        Station dest = transportData.getSecond();

        TramDate queryDate = TramTransportDataForTestFactory.startDate; //   TramDate.of(2014,6,30);
        TramTime queryTime = TramTime.of(7, 57);

        JourneyRequest request = standardJourneyRequest(queryDate, queryTime, 1);
        Running running = () -> true;

        request.setDiag(true);

        List<Journey> result = tramRouteCalculator.calculateRoute(txn, begin, dest, request, running).toList();

        assertFalse(result.isEmpty());

    }

    @Test
    void shouldDoInMemoryRouteCalc() {
        TramRouteCalculator tramRouteCalculator = componentContainer.get(TramRouteCalculator.class);

        Station begin = transportData.getFirst();
        Station dest = transportData.getLast();

        TramDate queryDate = TramTransportDataForTestFactory.startDate;
        TramTime queryTime = TramTime.of(7, 57);

        JourneyRequest request = standardJourneyRequest(queryDate, queryTime, 1);
        Running running = () -> true;

        request.setDiag(true);

        List<Journey> result = tramRouteCalculator.calculateRoute(txn, begin, dest, request, running).toList();

        assertFalse(result.isEmpty());

    }

    @Test
    void shouldDoInMemoryRouteCalcWithChange() {
        TramRouteCalculator tramRouteCalculator = componentContainer.get(TramRouteCalculator.class);

        Station begin = transportData.getFirst();
        Station dest = transportData.getFifthStation();

        TramDate queryDate = TramTransportDataForTestFactory.startDate;
        TramTime queryTime = TramTime.of(7, 57);

        JourneyRequest request = standardJourneyRequest(queryDate, queryTime, 1);
        Running running = () -> true;

        request.setDiag(true);

        List<Journey> result = tramRouteCalculator.calculateRoute(txn, begin, dest, request, running).toList();

        assertFalse(result.isEmpty());

    }

    @Test
    void shouldHaveWalkDirectFromStart() {
        TramDate date = TramTransportDataForTestFactory.startDate;

        TramTime queryTime = TramTime.of(7, 57);

        final JourneyRequest journeyRequest = standardJourneyRequest(date, queryTime, 2);
        final Location<?> start = nearWythenshaweHosp.location();
        final Station destination = transportData.getSecond();

        TramDuration walkCost = getWalkCost(start, destination);
        assertEquals(TramDuration.ofMinutes(3).plusSeconds(19), walkCost);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                journeyRequest, 2);

        assertFalse(journeys.isEmpty());
        //assertEquals(1, journeys.size(), "wrong number " + journeys);
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage<?, ?> walk = journey.getStages().getFirst();
            assertEquals(TransportMode.Walk, walk.getMode());
            assertEquals(destination, walk.getLastStation());
            assertEquals(queryTime, walk.getFirstDepartureTime());
            TestEnv.assertMinutesRoundedEquals(walkCost, walk.getDuration());
        });
    }

    private TramDuration getWalkCost(Location<?> start, Station destination) {
        return geography.getWalkingDuration(start, destination);
    }

    private JourneyRequest standardJourneyRequest(TramDate date, TramTime time, int maxNumberChanges) {
        TramDuration maxDuration = TramDuration.ofMinutes(config.getMaxJourneyDuration());
        return new JourneyRequest(date, time, false, maxNumberChanges, maxDuration,
                3, EnumSet.of(Tram));
    }

}
