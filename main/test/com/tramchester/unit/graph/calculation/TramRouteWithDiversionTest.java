package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.*;
import com.tramchester.graph.search.LocationJourneyPlanner;
import com.tramchester.graph.search.diagnostics.RecordJourneyGraphPath;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.config.TemporaryStationsWalkConfigForTest;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.DiagramCreator;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.KnownLocations.nearWythenshaweHosp;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.*;
import static org.junit.jupiter.api.Assertions.*;

class TramRouteWithDiversionTest {

    private static ComponentContainer componentContainer;
    private static LocationJourneyPlannerTestFacade locationJourneyPlanner;
    private static SimpleGraphWithWalkConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private RouteCalculatorTestFacade calculator;
    private Geography geography;

    private TramTime queryTime;
    private MutableGraphTransaction txn;
    private ImmutableEnumSet<TransportMode> modes;
    private TramDate baseDate;
    private TramDate withinDiversion;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {

        config = new SimpleGraphWithWalkConfig();
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

        baseDate = TramTransportDataForTestFactory.startDate;
        TramDate candidate = config.tempWalkDateRange.getStartDate().plusDays(3);
        while(candidate.getDayOfWeek()!= TramTransportDataForTestFactory.dayOfWeek) {
            candidate = candidate.plusDays(1);
        }
        withinDiversion = candidate;
        queryTime = TramTime.of(7, 57);
        StationRepository stationRepo = componentContainer.get(StationRepository.class);

        geography = componentContainer.get(Geography.class);

        modes = TransportMode.TramsOnly;

        txn = database.beginTxMutable();

        locationJourneyPlanner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class),
                stationRepo, txn);

        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, TramDate queryDate) {
        final int maxChanges = config.getMaxNumberChanges();
        return new JourneyRequest(queryDate, queryTime, false, maxChanges,
                TramDuration.ofMinutes(config.getMaxJourneyDuration()), 3, modes);
    }

    @AfterEach
    void afterEachTestRuns()
    {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldHaveRoutesSetupCorrectly() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);

        Set<Route> running = routeRepository.getRoutesRunningOn(baseDate, TransportMode.TramsOnly);

        assertEquals(routeRepository.numberOfRoutes(), running.size());
    }

    @Test
    void shouldTestSimpleJourneyIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, baseDate);
        Set<Journey> journeys = calculateRoute(transportData.getFirst(),
                transportData.getSecond(), journeyRequest).
                collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, Station.createId(FIRST_STATION), Station.createId(SECOND_STATION), 0, queryTime);

        Journey journey = journeys.iterator().next();
        final TransportStage<?, ?> transportStage = journey.getStages().getFirst();

        assertEquals(transportData.getFirst(), transportStage.getFirstStation());
        assertEquals(transportData.getSecond(), transportStage.getLastStation());
        assertEquals(0, transportStage.getPassedStopsCount());
        assertEquals(transportData.getRouteA().getShortName(),
                transportStage.getRoute().getShortName());
        assertEquals(transportStage.getFirstStation(), transportStage.getActionStation());
        assertMinutesEquals(11, transportStage.getDuration());
        assertEquals(TramTime.of(8,0), transportStage.getFirstDepartureTime());
        assertEquals(TramTime.of(8,11), transportStage.getExpectedArrivalTime()); // +1 for dep cost
    }

    @Test
    void shouldFollowDiversion() {

        RecordJourneyGraphPath recordJourneyGraphPath = componentContainer.get(RecordJourneyGraphPath.class);
        recordJourneyGraphPath.enable();

        final int maxChanges = config.getMaxNumberChanges();

        JourneyRequest journeyRequest = new JourneyRequest(withinDiversion, queryTime, false, maxChanges,
                TramDuration.ofMinutes(config.getMaxJourneyDuration()), 3, modes);

        Set<Journey> journeys = calculateRoute(transportData.getFirst(),
                transportData.getLast(), journeyRequest).
                collect(Collectors.toSet());
        assertEquals(1, journeys.size());

        Journey journey = journeys.iterator().next();
        final TransportStage<?, ?> transportStage = journey.getStages().getFirst();

        assertEquals(transportData.getFirst(), transportStage.getFirstStation());
        assertEquals(transportData.getLast(), transportStage.getLastStation());
        assertEquals(TransportMode.Connect, transportStage.getTransportMode());

        assertEquals(0, transportStage.getPassedStopsCount(), transportStage.toString());
        assertEquals("Walk", transportStage.getRoute().getShortName());
        assertEquals(transportStage.getFirstStation(), transportStage.getActionStation());
        assertEquals(TramDuration.ofMinutes(1), transportStage.getDuration());
        assertEquals(TramTime.of(7,57), transportStage.getFirstDepartureTime());
        assertEquals(TramTime.of(7,58), transportStage.getExpectedArrivalTime()); // +1 for dep cost

        // Check underlying path here
        List<TimedPath> timedPaths = recordJourneyGraphPath.getPathsFor(journeyRequest.getUid());
        assertEquals(1, timedPaths.size());

        TimedPath timedPath = timedPaths.getFirst();

        GraphPath graphPath = timedPath.path();

        GraphNode startNode = txn.findNode(transportData.getFirst());
        GraphNode endNode = txn.findNode(transportData.getLast());

        assertEquals(startNode, graphPath.getStartNode(txn));
        assertEquals(endNode, graphPath.getEndNode(txn));

        //Iterable<GraphEntity<? extends GraphId>> allEntities = graphPath.getEntities(txn);

        List<GraphEntity<? extends GraphId>> entityList = graphPath.getEntityStream(txn).toList();

        assertEquals(3, entityList.size(), "Wrong length " + graphPath.displayPath());

        assertTrue(entityList.get(1).isRelationship());

        GraphRelationship diversion = (GraphRelationship) entityList.get(1);
        assertEquals(startNode, diversion.getStartNode(txn));
        assertEquals(endNode, diversion.getEndNode(txn));

    }

    private Stream<Journey> calculateRoute(Location<?> first, Location<?> second, JourneyRequest journeyRequest) {
        return calculator.calculateRouteAsList(first, second, journeyRequest).stream();
    }

    @Test
    void shouldHaveJourneyWithLocationViaDiversion() {
        Location<?> origin = nearAltrincham.location();

        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(7, 57), withinDiversion);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(origin,  transportData.getLast(),
                journeyRequest, 3);

        assertEquals(1, journeys.size(), journeys.toString());
        journeys.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(1, stages.size(), "stages: " + stages);
            assertEquals(TransportMode.Walk, stages.getFirst().getTransportMode());
            //assertEquals(Connect, stages.get(1).getTransportMode(), stages.toString());
        });
    }

    @Test
    void shouldHaveJourneyWithLocationBasedStart() {
        final Location<?> start = nearWythenshaweHosp.location();
        final Station destination = transportData.getInterchange();
        final Station midway = transportData.getSecond();

        TramDuration walkCost = getWalkCost(start, midway);
        TestEnv.assertMinutesRoundedEquals(TramDuration.ofMinutes(3), walkCost);

        int tramDur = 9;
        TramTime tramBoard = TramTime.of(8,11);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                createJourneyRequest(queryTime, withinDiversion), 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size());

            final TransportStage<?, ?> walk = stages.get(0);
            final TransportStage<?, ?> tram = stages.get(1);

            assertEquals(midway, walk.getLastStation());
            assertEquals(TransportMode.Walk, walk.getTransportMode());
            TestEnv.assertMinutesRoundedEquals(walkCost, walk.getDuration());

            assertEquals(tramBoard.minusRounded(walkCost),
                    walk.getFirstDepartureTime(), tramBoard + " minus " + walkCost + " = " + tramBoard.minusRounded(walkCost));

            assertEquals(tramBoard, walk.getExpectedArrivalTime());

            assertEquals(midway, tram.getFirstStation());
            assertEquals(destination, tram.getLastStation());
            assertMinutesEquals(tramDur, tram.getDuration());
            assertEquals(tramBoard, tram.getFirstDepartureTime());
            assertEquals(tramBoard.plusMinutes(tramDur), tram.getExpectedArrivalTime());
        });
    }

    @Test
    void shouldHaveWalkDirectFromStart() {
        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, withinDiversion);
        final Location<?> start = nearWythenshaweHosp.location();
        final Station destination = transportData.getSecond();

        TramDuration walkCost = getWalkCost(start, destination);
        assertEquals(TramDuration.ofMinutes(3).plusSeconds(19), walkCost);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                journeyRequest, 2);

        assertEquals(1, journeys.size(), "wrong number " + journeys);
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage<?, ?> walk = journey.getStages().getFirst();
            assertEquals(TransportMode.Walk, walk.getTransportMode());
            assertEquals(destination, walk.getLastStation());
            assertEquals(queryTime, walk.getFirstDepartureTime());
            TestEnv.assertMinutesRoundedEquals(walkCost, walk.getDuration());
        });
    }

    private TramDuration getWalkCost(Location<?> start, Station destination) {
        return geography.getWalkingDuration(start, destination);
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchangeFromSecondStation() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, withinDiversion);

        Set<Journey> journeys = calculateRoute(transportData.getSecond(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());

        assertEquals(1, journeys.size(), journeys.toString());
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, withinDiversion);

        Set<Journey> journeys = calculateRoute(transportData.getFirst(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, Station.createId(FIRST_STATION), Station.createId(INTERCHANGE), 1, queryTime);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

    private void checkForPlatforms(Set<Journey> journeys) {
        journeys.forEach(journey -> journey.getStages().
                forEach(stage -> assertTrue(stage.hasBoardingPlatform(), "Missing boarding platform for " + stage)));
    }



    @Test
    void shouldHaveRouteCostCalculationAsExpected() throws InvalidDurationException {
        RouteCostCalculator costCalculator = componentContainer.get(RouteCostCalculator.class);
        assertMinutesEquals(41, costCalculator.getAverageCostBetween(txn,
                transportData.getFirst(), transportData.getLast(), withinDiversion, modes));

    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Assertions.assertAll(() -> creator.create(Path.of("test_network_diversions.dot"),
                transportData.getFirst(), 100, false));
    }

    private static void assertFirstAndLast(Set<Journey> journeys, IdFor<Station> firstStation, IdFor<Station> secondStation,
                                           int passedStops, TramTime queryTime) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage<?,?>> stages = journey.getStages();
        TransportStage<?,?> vehicleStage = stages.getFirst();
        assertEquals(firstStation, vehicleStage.getFirstStation().getId());
        assertEquals(secondStation, vehicleStage.getLastStation().getId());
        assertEquals(passedStops,  vehicleStage.getPassedStopsCount());
        assertTrue(vehicleStage.hasBoardingPlatform(), "Missing boarding platform in " + vehicleStage);

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        assertTrue(departTime.isAfter(queryTime));

        assertTrue(vehicleStage.getDuration().isValid());
        assertFalse(vehicleStage.getDuration().isZero());
    }

    private static class SimpleGraphWithWalkConfig extends UnitTestOfGraphConfig {
        private final List<TemporaryStationsWalkIds> temporaryWalks;
        private final DateRange tempWalkDateRange;

        public SimpleGraphWithWalkConfig() {
            super();
            // config is created beofre other components, so have to use the static ref data her
            TramDate startOfRange = TramTransportDataForTestFactory.endDate.minusWeeks(8);

            tempWalkDateRange = DateRange.of(startOfRange, startOfRange.plusWeeks(2));
            StationIdPair stationPair = StationIdPair.of(Station.createId(FIRST_STATION),
                    Station.createId(LAST_STATION));
            temporaryWalks = List.of(
                    new TemporaryStationsWalkConfigForTest(stationPair, tempWalkDateRange,
                            TramDuration.ofMinutes(1))
            );
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            final Set<TransportMode> compositeStationModes = Collections.singleton(Tram);
            final ImmutableIdSet<Station> additionalInterchanges = IdSet.singleton(TramStations.Cornbrook.getId());
            TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig(
                    GTFSTransportationType.tram, Tram, additionalInterchanges, compositeStationModes,
                    Collections.emptyList(), TramDuration.ofMinutes(13), temporaryWalks);
            return Collections.singletonList(tfgmTestDataSourceConfig);
        }
    }
}
