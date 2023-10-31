package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.KnownLocations.*;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.*;
import static org.junit.jupiter.api.Assertions.*;

class TramRouteTest {

    private static ComponentContainer componentContainer;
    private static LocationJourneyPlannerTestFacade locationJourneyPlanner;
    private static SimpleGraphConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private RouteCalculator calculator;
    private Geography geography;

    private TramDate queryDate;
    private TramTime queryTime;
    private GraphTransaction txn;
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleCompositeGraphConfig("tramroutetest.db");
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
        calculator = componentContainer.get(RouteCalculator.class);

        queryDate = TramDate.of(2014,6,30);
        queryTime = TramTime.of(7, 57);
        StationRepository stationRepo = componentContainer.get(StationRepository.class);

        geography = componentContainer.get(Geography.class);

        modes = TramsOnly;

        txn = database.beginTx();

        locationJourneyPlanner = new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class),
                stationRepo, txn);
    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 3, modes);
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

        Set<Route> running = routeRepository.getRoutesRunningOn(queryDate);

        assertEquals(routeRepository.numberOfRoutes(), running.size());
    }

    @Test
    void shouldTestSimpleJourneyIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getSecond(), journeyRequest).
                collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, Station.createId(FIRST_STATION), Station.createId(SECOND_STATION), 0, queryTime);

        Journey journey = journeys.iterator().next();
        final TransportStage<?, ?> transportStage = journey.getStages().get(0);

        assertEquals(transportData.getFirst(), transportStage.getFirstStation());
        assertEquals(transportData.getSecond(), transportStage.getLastStation());
        assertEquals(0, transportStage.getPassedStopsCount());
        assertEquals(KnownTramRoute.CornbrookTheTraffordCentre.shortName(), transportStage.getRoute().getShortName());
        assertEquals(transportStage.getFirstStation(), transportStage.getActionStation());
        assertMinutesEquals(11, transportStage.getDuration());
        assertEquals(TramTime.of(8,0), transportStage.getFirstDepartureTime());
        assertEquals(TramTime.of(8,11), transportStage.getExpectedArrivalTime()); // +1 for dep cost
    }

    @Test
    void shouldHaveJourneyWithLocationBasedStartViaComposite() {
        Location<?> origin = nearAltrincham.location();

        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(7, 57), 0);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(origin,  transportData.getSecond(),
                journeyRequest, 3);

        assertEquals(1, journeys.size(), journeys.toString());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size(), "stages: " + stages);
            assertEquals(stages.get(0).getMode(), TransportMode.Walk);
            assertEquals(stages.get(1).getMode(), TransportMode.Tram);
        });
    }

    @Test
    void shouldHaveJourneyWithLocationBasedStart() {
        final Location<?> start = nearWythenshaweHosp.location();
        final Station destination = transportData.getInterchange();
        final Station midway = transportData.getSecond();

        int walkCost = getWalkCost(start, midway);
        assertEquals(4, walkCost);

        int tramDur = 9;
        TramTime tramBoard = TramTime.of(8,11);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                createJourneyRequest(queryTime, 0), 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size());

            final TransportStage<?, ?> walk = stages.get(0);
            final TransportStage<?, ?> tram = stages.get(1);

            assertEquals(midway, walk.getLastStation());
            assertEquals(walk.getMode(), TransportMode.Walk);
            assertMinutesEquals(walkCost, walk.getDuration());
            final int boardAndPlatformEntry =  1;
            assertEquals(tramBoard.minusMinutes(boardAndPlatformEntry + walkCost), walk.getFirstDepartureTime(), journey.toString());
            assertEquals(tramBoard.minusMinutes(boardAndPlatformEntry), walk.getExpectedArrivalTime());

            assertEquals(midway, tram.getFirstStation());
            assertEquals(destination, tram.getLastStation());
            assertMinutesEquals(tramDur, tram.getDuration());
            assertEquals(tramBoard, tram.getFirstDepartureTime());
            assertEquals(tramBoard.plusMinutes(tramDur), tram.getExpectedArrivalTime());
        });
    }

    @Test
    void shouldHaveWalkDirectFromStart() {
        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        final Location<?> start = nearWythenshaweHosp.location();
        final Station destination = transportData.getSecond();

        int walkCost = getWalkCost(start, destination);
        assertEquals(4, walkCost);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                journeyRequest, 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage<?, ?> walk = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, walk.getMode());
            assertEquals(destination, walk.getLastStation());
            assertEquals(queryTime, walk.getFirstDepartureTime());
            assertMinutesEquals(walkCost, walk.getDuration());
        });
    }

    private int getWalkCost(Location<?> start, Station destination) {
        Duration duration = geography.getWalkingDuration(start, destination);
        return (int) Math.ceil(duration.getSeconds()/60D);
    }

    @Test
    void shouldHaveWalkDirectAtEnd() {
        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);
        final Station start = transportData.getSecond();
        final Location<?> destination = nearWythenshaweHosp.location();

        int walkCost = getWalkCost(destination, start);
        assertEquals(4, walkCost);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination,
                journeyRequest, 2);

        assertEquals(1, journeys.size());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage<?, ?> walk = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, walk.getMode());
            assertEquals(start, walk.getFirstStation());
            assertEquals(queryTime, walk.getFirstDepartureTime());
            assertMinutesEquals(walkCost, walk.getDuration());
        });
    }

    @Test
    void shouldHaveWalkAtStartAndEnd() {
        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 2);

        final Location<?> start = nearWythenshaweHosp.location();
        final Location<?> destination = atMancArena.location();

        final Station endFirstWalk = transportData.getSecond();
        final Station startSecondWalk = transportData.getInterchange();

        int walk1Cost = getWalkCost(start, endFirstWalk);
        int walk2Cost = getWalkCost(destination, startSecondWalk);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start, destination, journeyRequest, 3);
        assertTrue(journeys.size() >= 1, "journeys");
        journeys.forEach(journey -> {
            assertEquals(3, journey.getStages().size());
            TransportStage<?, ?> walk1 = journey.getStages().get(0);
            TransportStage<?, ?> tram = journey.getStages().get(1);
            TransportStage<?, ?> walk2 = journey.getStages().get(2);

            assertEquals(TransportMode.Walk, walk1.getMode());
            assertEquals(TransportMode.Walk, walk2.getMode());
            assertEquals(TransportMode.Tram, tram.getMode());

            assertMinutesEquals(walk1Cost, walk1.getDuration());
            assertMinutesEquals(walk2Cost, walk2.getDuration());

            assertEquals(endFirstWalk, walk1.getLastStation());
            assertEquals(endFirstWalk, tram.getFirstStation());
            assertEquals(startSecondWalk, tram.getLastStation());
            assertEquals(startSecondWalk, walk2.getFirstStation());

            assertTrue(tram.getFirstDepartureTime().isAfter(walk1.getExpectedArrivalTime()) ||
                    tram.getFirstDepartureTime().equals(walk1.getExpectedArrivalTime()), "tram after walk 1");
            assertTrue(tram.getExpectedArrivalTime().isBefore(walk2.getFirstDepartureTime()) ||
                    tram.getExpectedArrivalTime().equals(walk2.getFirstDepartureTime()), "walk 2 affter tram");

            });
    }

    @Test
    void shouldHaveJourneyWithLocationBasedEnd() {

        final JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        final Location<?> destination = atMancArena.location();
        final Station start = transportData.getSecond();
        final Station midway = transportData.getInterchange();

        int walkCost = getWalkCost(destination, midway);
        assertEquals(5, walkCost);

        TramTime boardTime = TramTime.of(8,11);
        final int tramDuration = 9;
        final int depart = 0;

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(start,
                destination,
                journeyRequest, 3);

        assertEquals(2, journeys.size());
        journeys.forEach(journey ->{
            List<TransportStage<?,?>> stages = journey.getStages();
            assertEquals(2, stages.size());
            final TransportStage<?, ?> tram = stages.get(0);
            final TransportStage<?, ?> walk = stages.get(1);

            assertEquals(tram.getFirstStation(), start);
            assertEquals(tram.getLastStation(), midway);
            assertEquals(tram.getFirstDepartureTime(), boardTime);
            assertEquals(tram.getExpectedArrivalTime(), boardTime.plusMinutes(tramDuration));

            assertEquals(walk.getMode(), TransportMode.Walk);
            assertEquals(walk.getFirstStation(), midway);
            assertMinutesEquals(walkCost, walk.getDuration());
            assertEquals(walk.getFirstDepartureTime(), boardTime.plusMinutes(tramDuration+depart));
            assertEquals(boardTime.plusMinutes(tramDuration+depart+walkCost), walk.getExpectedArrivalTime());

            assertTrue(walk.getFirstDepartureTime().isAfter(tram.getExpectedArrivalTime()) ||
                            walk.getFirstDepartureTime().equals(tram.getExpectedArrivalTime()),
                    tram.getExpectedArrivalTime().toString());

        });
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchangeFromSecondStation() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getSecond(),
                transportData.getInterchange(), journeyRequest).collect(Collectors.toSet());

        assertEquals(2, journeys.size(), journeys.toString());
    }

    @Test
    void shouldTestSimpleJourneyIsPossibleToInterchange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
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
    void shouldTestSimpleJourneyIsNotPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(10, 0), 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(),
                journeyRequest).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), journeys);
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getLast(), journeyRequest).collect(Collectors.toSet());
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, Station.createId(FIRST_STATION), Station.createId(LAST_STATION), 2, queryTime);
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

    @Test
    void shouldTestNoJourneySecondToStart() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getSecond(),
                transportData.getFirst(), journeyRequest).collect(Collectors.toSet());
        assertEquals(0,journeys.size());
    }

    @Test
    void shouldTestJourneyInterchangeToFive() {
        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(7,56), 0);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertFalse(journeys.size()>=1);

        JourneyRequest journeyRequestB = createJourneyRequest(TramTime.of(8, 10), 3);
        journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequestB).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFourthStation(), journeyRequest).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> assertEquals(2, journey.getStages().size()));
    }

    @Test
    void shouldReturnZeroJourneysIfStartOutOfRange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getFirst(),
                nearGreenwichLondon, journeyRequest,3);
        assertTrue(journeys.isEmpty());
    }

    @Test
    void shouldReturnZeroJourneysIfDestOutOfRange() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(nearGreenwichLondon,
                transportData.getFirst(), journeyRequest,3);
        assertTrue(journeys.isEmpty());
    }

    @Test
    void shouldTestJourneyEndOverWaitLimitViaInterchangeLocationFinishIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);

        // nearStockportBus == station 5
        Set<Journey> journeys = locationJourneyPlanner.quickestRouteForLocation(transportData.getFirst(),
                nearStockportBus, journeyRequest,3);

        assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> {
            assertEquals(3, journey.getStages().size(), journey.getStages().toString());
            assertEquals(transportData.getInterchange(), journey.getStages().get(0).getLastStation());
            assertEquals(transportData.getFifthStation(), journey.getStages().get(1).getLastStation());
            assertTrue(journey.getStages().get(2) instanceof WalkingStage);
        });
    }

    @Test
    void shouldTestJourneyAnotherWaitLimitViaInterchangeIsPossible() {
        JourneyRequest journeyRequest = createJourneyRequest(queryTime, 1);
        List<Station> expectedPath = Arrays.asList(transportData.getFirst(),
                transportData.getSecond(), transportData.getInterchange(), transportData.getFifthStation());

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        checkForPlatforms(journeys);
        journeys.forEach(journey-> {
            assertEquals(2, journey.getStages().size());
            final TransportStage<?, ?> firstStage = journey.getStages().get(0);
            final TransportStage<?, ?> secondStage = journey.getStages().get(1);

            assertEquals(1, firstStage.getPassedStopsCount());
            assertMinutesEquals(11+9, firstStage.getDuration());

            assertEquals(0, secondStage.getPassedStopsCount());
            assertMinutesEquals(4, secondStage.getDuration());
            assertEquals(expectedPath, journey.getPath());

        });
    }

    @Test
    void shouldHaveRouteCostCalculationAsExpected() throws InvalidDurationException {
        RouteCostCalculator costCalculator = componentContainer.get(RouteCostCalculator.class);
        assertMinutesEquals(41, costCalculator.getAverageCostBetween(txn,
                transportData.getFirst(), transportData.getLast(), queryDate, modes));

//        assertEquals(-1, costCalculator.getAverageCostBetween(txn, transportData.getLast(), transportData.getFirst(), queryDate));
    }

    @Test
    void shouldThrowIfDurationIsInvalid() {
        RouteCostCalculator costCalculator = componentContainer.get(RouteCostCalculator.class);

        assertThrows(InvalidDurationException.class,
                () -> costCalculator.getAverageCostBetween(txn, transportData.getLast(), transportData.getFirst(), queryDate, modes));
    }

    @Test
    void createDiagramOfTestNetwork() {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        Assertions.assertAll(() -> creator.create(Path.of("test_network.dot"),
                transportData.getFirst(), 100, false));
    }

    private static void assertFirstAndLast(Set<Journey> journeys, IdFor<Station> firstStation, IdFor<Station> secondStation,
                                           int passedStops, TramTime queryTime) {
        Journey journey = (Journey)journeys.toArray()[0];
        List<TransportStage<?,?>> stages = journey.getStages();
        TransportStage<?,?> vehicleStage = stages.get(0);
        assertEquals(firstStation, vehicleStage.getFirstStation().getId());
        assertEquals(secondStation, vehicleStage.getLastStation().getId());
        assertEquals(passedStops,  vehicleStage.getPassedStopsCount());
        assertTrue(vehicleStage.hasBoardingPlatform(), "Missing boarding platform in " + vehicleStage);

        TramTime departTime = vehicleStage.getFirstDepartureTime();
        assertTrue(departTime.isAfter(queryTime));

        assertFalse(vehicleStage.getDuration().isNegative());
        assertFalse(vehicleStage.getDuration().isZero());
    }

}
