package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.*;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static com.tramchester.testSupport.TestEnv.NoopRegisterMetrics;
import static com.tramchester.testSupport.TestEnv.deleteDBIfPresent;
import static com.tramchester.testSupport.reference.KnownLocality.MIN_CHANGES;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@BusTest
class BusRouteCalculatorSubGraphAltyToMaccRouteTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private static Set<Route> altyToMacc;

    private RouteCalculatorTestFacade calculator;

    private ImmutableGraphTransactionNeo4J txn;
    private TramDate when;
    private StationGroupsRepository stationGroupsRepository;
    private StationLocalityGroup altrinchamInterchange;
    private List<Station> knutfordStations;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {

        config = new SubgraphConfig();
        deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(BusRouteCalculatorSubGraphAltyToMaccRouteTest::configureFilter).
                create(config, NoopRegisterMetrics());
        componentContainer.initialise();

    }

    private static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        altyToMacc.forEach(route -> graphFilter.addRoute(route.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        RouteRepository routeRepository = componentContainer.get(TransportData.class);
        AgencyRepository agencyRepository = componentContainer.get(AgencyRepository.class);
        IdFor<Agency> agencyId = agencyRepository.findByName("D&G Bus");
        altyToMacc = routeRepository.findRoutesByName(agencyId,
                "Altrincham - Wilmslow - Knutsford - Macclesfield");
        assertFalse(altyToMacc.isEmpty());

        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);

        txn = database.beginTx();

        calculator = new RouteCalculatorTestFacade(componentContainer, txn);

        when = TestEnv.testDay();

        StationLocations stationLocations = componentContainer.get(StationLocations.class);

        // because knutford bus station is just called "Bus Station"
        // TODO USE BusStations.KnutfordStationAreaId here
        //LatLong nearKnutsfordBusStation = new LatLong(53.30262,-2.3775267);

        final MarginInMeters rangeInMeters = MarginInMeters.ofMeters(1000);
        knutfordStations = stationLocations.nearestStationsSorted(KnownLocations.nearKnutsfordBusStation.location(),
                        10, rangeInMeters, config.getTransportModes()).
                stream().
                filter(station -> station.getName().contains("Bus Station")).
                collect(Collectors.toList());

        altrinchamInterchange =  KnownLocality.Altrincham.from(stationGroupsRepository);

    }

    @AfterEach
    void afterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldHaveKnutfordBusStation() {
        assertFalse(knutfordStations.isEmpty());
    }

    @Test
    void shouldFindRoutesForTest() {
        assertNotNull(altyToMacc);
    }

    @Test
    void shouldHavePickupAndDropoffForStationGroup() {
        assertNotNull(altrinchamInterchange);

        assertFalse(altrinchamInterchange.getPickupRoutes().isEmpty());
        assertFalse(altrinchamInterchange.getDropoffRoutes().isEmpty());
    }

    @Test
    void shouldHaveJourneyAltyToKnutsford() {
        Station end = knutfordStations.get(0);

        TramTime time = TramTime.of(10, 40);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1,
                Duration.ofMinutes(120), 2, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(altrinchamInterchange, end, journeyRequest);

        assertFalse(results.isEmpty());
    }

    private EnumSet<TransportMode> getRequestedModes() {
        return BusesOnly;
    }

    @Test
    void shouldHaveJourneyKnutsfordToAlty() {
        // NOTE: can cause (ignorable) errors on destination station node ID search as some of the these stations are
        // not on the specified filtered routes, so not present in the DB

        //Station start = compositeStationRepository.findByName("Bus Station");
        Station start = knutfordStations.get(0);
        assertNotNull(start, stationGroupsRepository.getAllGroups().toString());

        TramTime time = TramTime.of(11, 20);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                3, Duration.ofMinutes(120), 2, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(start, altrinchamInterchange, journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveKnownGoodJourney() {
        Station start = stationRepository.getStationById(Station.createId("0600MA6001")); // "The Towers" CHEPJWM
        Station dest = stationRepository.getStationById(Station.createId("0600MA0175")); // "Grove Park" CHEMJDT

        TramTime time = TramTime.of(12, 40);
        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, BusesOnly);

        List<Journey> results = calculator.calculateRouteAsList(start, dest, request);
        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveMacclesfieldToKnutsford() {
        TramTime time = TramTime.of(12, 20);
        JourneyRequest request = new JourneyRequest(when, time, false, MIN_CHANGES,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, BusesOnly);

        List<Journey> results = calculator.calculateRouteAsList(KnownLocality.Macclesfield, KnownLocality.Knutsford,
                request);
        assertFalse(results.isEmpty());
    }

    @Disabled("WIP")
    @Test
    void shouldHaveSimpleRouteWithStationsAlongTheWay() {

        // TODO WIP

        altyToMacc.forEach(route -> {
            //List<Station> stationsAlongRoute = routeCallingStations.getStationsFor(route);
            route.getTrips().forEach(trip -> {
                StopCalls stopCalls = trip.getStopCalls();

                List<IdFor<Station>> ids = stopCalls.stream().map(stopCall -> stopCall.getStation().getId()).toList();

                int knutsfordIndex = ids.indexOf(Station.createId("0600MA6022")); // services beyond here are infrequent
                Station firstStation = stopCalls.getFirstStop(true).getStation();

                TramTime time = TramTime.of(9, 20);
                JourneyRequest journeyRequest = new JourneyRequest(when, time, false,
                        1, Duration.ofMinutes(120), 1, getRequestedModes());

                for (int i = 1; i <= knutsfordIndex; i++) {
                    StopCall stopCall = stopCalls.getStopBySequenceNumber(i);
                    Station secondStation = stopCall.getStation();
                    List<Journey> result = calculator.calculateRouteAsList(firstStation, secondStation, journeyRequest);
                    assertFalse(result.isEmpty());
                }
            });


        });

    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        // all station for both sets of routes
        Set<Station> stations = altyToMacc.stream().
                flatMap(route -> route.getTrips().stream()).
                flatMap(trip -> trip.getStopCalls().getStationSequence(false).stream()).
                collect(Collectors.toSet());
        creator.create(Path.of("AltrichamKnutsfordBuses.dot"), stations, 1, true);
    }

    private static class SubgraphConfig extends IntegrationBusTestConfig {
        @Override
        public boolean isGraphFiltered() {
            return true;
        }
    }
}
