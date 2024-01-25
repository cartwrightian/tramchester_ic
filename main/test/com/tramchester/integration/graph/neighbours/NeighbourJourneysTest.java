package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.IntegrationTramBusTestConfig;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.BusStations.KnutsfordStationStand3;
import static com.tramchester.testSupport.reference.BusStations.StockportNewbridgeLane;
import static com.tramchester.testSupport.reference.KnownLocations.nearStPetersSquare;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
public class NeighbourJourneysTest {
    private StationRepository stationRepository;
    private static RouteCalculatorTestFacade routeCalculator;
    private static TramchesterConfig config;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private MutableGraphTransaction txn;
    private Station shudehillBusStop;
    private LocationJourneyPlanner planner;
    private RouteToRouteCosts routeToRouteCosts;
    private Duration maxJourneyDuration;
    private TramDate date;
    private TimeRange timeRange;
    private final EnumSet<TransportMode> modes = EnumSet.of(Bus, Tram);

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramBusTestConfig(true);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        stationRepository = componentContainer.get(StationRepository.class);

        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

        StationGroup shudehillCentralBus = KnownLocality.Shudehill.from(stationGroupsRepository);

        Optional<Station> maybeStop = shudehillCentralBus.getAllContained().stream().findAny();
        maybeStop.ifPresent(stop -> shudehillBusStop = stop);

        shudehillTram = stationRepository.getStationById(Shudehill.getId());

        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());

        txn = graphDatabase.beginTxMutable();
        routeCalculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        planner = componentContainer.get(LocationJourneyPlanner.class);

        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

        date = TestEnv.testDay();

        timeRange = TimeRange.of(TramTime.of(8,15), TramTime.of(22,35));

    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldHaveTestStations() {
        assertNotNull(shudehillBusStop);
        assertNotNull(shudehillTram);
    }

    @Test
    void shouldHaveCorrectRouteToRouteHopsWhenNeighbours() {

        NumberOfChanges busToTramHops = routeToRouteCosts.getNumberOfChanges(shudehillBusStop, shudehillTram, modes, date, timeRange);
        assertEquals(1, busToTramHops.getMin());
        assertEquals(1, busToTramHops.getMax());

        NumberOfChanges tramToBusHops = routeToRouteCosts.getNumberOfChanges(shudehillTram, shudehillBusStop, modes, date, timeRange);
        assertEquals(1, tramToBusHops.getMin());
        assertEquals(1, tramToBusHops.getMax());
    }

    @Test
    void shouldHaveCorrectRouteToRouteHopsWhenNeighboursSets() {

        LocationSet trams = new LocationSet(Arrays.asList(Altrincham.from(stationRepository), HarbourCity.from(stationRepository)));

        LocationSet buses = new LocationSet(Arrays.asList(KnutsfordStationStand3.from(stationRepository), StockportNewbridgeLane.from(stationRepository)));


        NumberOfChanges busToTramHops = routeToRouteCosts.getNumberOfChanges(buses, trams, date, timeRange, modes);
        assertEquals(1, busToTramHops.getMin());
        assertEquals(2, busToTramHops.getMax());

        NumberOfChanges tramToBusHops = routeToRouteCosts.getNumberOfChanges(trams, buses, date, timeRange, modes);
        assertEquals(1, tramToBusHops.getMin());
        assertEquals(2, tramToBusHops.getMax());

        // now add neighbouring stops
        trams.add(shudehillTram);
        buses.add(shudehillBusStop);

        busToTramHops = routeToRouteCosts.getNumberOfChanges(buses, trams, date, timeRange, modes);
        assertEquals(1, busToTramHops.getMin());
        assertEquals(1, busToTramHops.getMax());

        tramToBusHops = routeToRouteCosts.getNumberOfChanges(trams, buses, date, timeRange, modes);
        assertEquals(1, tramToBusHops.getMin());
        assertEquals(1, tramToBusHops.getMax());
    }

    @Test
    void shouldFindMaxRouteHopsBetweenModes() {
        NumberOfChanges hops = routeToRouteCosts.getNumberOfChanges(shudehillTram, shudehillBusStop, modes, date, timeRange);
        assertEquals(1, hops.getMax());
    }

    @Test
    void shouldHaveIntermodalNeighboursAsInterchanges() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        assertTrue(interchangeRepository.isInterchange(shudehillTram));
        assertTrue(interchangeRepository.isInterchange(shudehillBusStop));
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourTramToBus() {
        validateDirectWalk(shudehillTram, shudehillBusStop);
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourBusToTram() {
        validateDirectWalk(shudehillBusStop, shudehillTram);
    }

    @Test
    void shouldTramNormally() {

        JourneyRequest request = new JourneyRequest(TestEnv.testDay(),
                TramTime.of(11,53), false, 0, maxJourneyDuration, 1, modes);

        List<Journey> journeys = routeCalculator.calculateRouteAsList(Bury.from(stationRepository), Victoria.from(stationRepository), request);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(Tram, stage.getMode());
        });
    }

    @Test
    void shouldTramThenWalk() {

        LocationJourneyPlannerTestFacade facade = new LocationJourneyPlannerTestFacade(planner, stationRepository, txn);

        JourneyRequest request = new JourneyRequest(TestEnv.testDay(),
                TramTime.of(11,53), false, 1, maxJourneyDuration, 5, modes);

        Set<Journey> allJourneys = facade.quickestRouteForLocation(Altrincham.from(stationRepository), nearStPetersSquare, request, 4);
        assertFalse(allJourneys.isEmpty(), "No journeys");

        Set<Journey> maybeTram = allJourneys.stream().
                filter(journey -> journey.getTransportModes().contains(Tram)).
                collect(Collectors.toSet());

        assertFalse(maybeTram.isEmpty(), "No tram " + allJourneys);

        maybeTram.forEach(journey -> {
            final List<TransportStage<?, ?>> stages = journey.getStages();

            TransportStage<?,?> firstStage = stages.get(0);
            assertEquals(Tram, firstStage.getMode(), firstStage.toString());

            TransportStage<?,?> last = stages.get(stages.size()-1);
            assertEquals(TransportMode.Walk, last.getMode(), last.toString());
        });
    }

    private void validateDirectWalk(Station start, Station end) {

        JourneyRequest request = new JourneyRequest(TestEnv.testDay(), TramTime.of(11,45),
                        false, 0, maxJourneyDuration, 3, modes);

        List<Journey> allJourneys =  routeCalculator.calculateRouteAsList(start, end, request);
        assertFalse(allJourneys.isEmpty(), "no journeys");

        Set<Journey> journeys = allJourneys.stream().filter(Journey::isDirect).collect(Collectors.toSet());

        journeys.forEach(journey -> {
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(TransportMode.Connect, stage.getMode());
        });
    }


}
