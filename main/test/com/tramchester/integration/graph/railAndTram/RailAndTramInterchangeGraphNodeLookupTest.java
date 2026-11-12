package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.search.InterchangeGraphNodeLookup;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
public class RailAndTramInterchangeGraphNodeLookupTest {

    private static GuiceContainerDependencies componentContainer;
    private MutableGraphTransaction txn;

    private static final int TXN_TIMEOUT = 1;
    private StationRepository stationRepository;
    private InterchangeRepository interchangeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new RailAndTramGreaterManchesterConfig();

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);

    }

    @AfterEach
    void onceAfterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldNotAllowSingleModeChangeTram() {

        InterchangeGraphNodeLookup interchangeGraphLookup = new InterchangeGraphNodeLookup(txn, interchangeRepository, TramsOnly);
        Station station = TramStations.NavigationRoad.from(stationRepository);

//        Set<Route> dropOffs = station.getDropoffRoutes();
//        dropOffs.forEach(route -> {
//            RouteStation routeStation = stationRepository.getRouteStation(station, route);
//
//            GraphNode node = txn.findNode(routeStation);
//
//            GraphLabels labels = node.getLabels();
//            assertTrue(labels.contains(GraphLabel.INTERCHANGE), "missing from " + labels);
//            assertTrue(labels.contains(GraphLabel.TRAM), "missing from " + labels);
//            assertTrue(labels.contains(GraphLabel.INTERCHANGE_NOT_SAME_MODE), "missing from " + labels);
//        });

        assertTrue(interchangeRepository.isInterchange(station), "sanity check failed");

        validateFor(station, interchangeGraphLookup, false);
    }

    @Test
    void shouldNotAllowSingleModeChangeTrain() {

        InterchangeGraphNodeLookup interchangeGraphLookup = new InterchangeGraphNodeLookup(txn, interchangeRepository, TrainOnly);
        Station station = RailStationIds.EastDidsbury.from(stationRepository);

        assertTrue(interchangeRepository.isInterchange(station), "sanity check failed");

        validateFor(station, interchangeGraphLookup, false);
    }

    @Test
    void shouldAllowSingleModeChangeTram() {

        InterchangeGraphNodeLookup interchangeGraphLookup = new InterchangeGraphNodeLookup(txn, interchangeRepository, TramsOnly);

        Station station = TramStations.Victoria.from(stationRepository);

        assertTrue(interchangeRepository.isInterchange(station), "sanity check failed");

        validateFor(station, interchangeGraphLookup, true);
    }

    @Test
    void shouldAllowForTramWhenManyModesRequested() {

        ImmutableEnumSet<TransportMode> modes = ImmutableEnumSet.of(Train,Tram, TramReplacementBus);

        InterchangeGraphNodeLookup interchangeGraphLookup = new InterchangeGraphNodeLookup(txn, interchangeRepository, modes);
        Station station = NavigationRoad.from(stationRepository);

        assertTrue(interchangeRepository.isInterchange(station), "sanity check failed");

        validateFor(station, interchangeGraphLookup, true);
    }

    private void validateFor(Station station, InterchangeGraphNodeLookup interchangeGraphLookup, boolean expected) {
        station.getDropoffRoutes().forEach(route -> {
            RouteStation routeStation = stationRepository.getRouteStation(station, route);
            final GraphNode node = txn.findNode(routeStation);
            assertEquals(expected, interchangeGraphLookup.checkInterchangeAvailableFor(node),
                    "Incorrect for " + station.getId() + " " + route.getId());
        });
    }
}
