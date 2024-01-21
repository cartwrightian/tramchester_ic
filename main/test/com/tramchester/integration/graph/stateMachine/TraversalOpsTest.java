package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.ManAirport;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TraversalOpsTest {
    private static ComponentContainer componentContainer;

    private NodeContentsRepository nodeOperations;
    private TripRepository tripRepository;
    private StationRepository stationRepository;
    private MutableGraphTransaction txn;
    private RouteToRouteCosts routeToRouteCosts;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforEachTestRuns() {
        nodeOperations = componentContainer.get(NodeContentsRepository.class);
        tripRepository = componentContainer.get(TripRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTxMutable();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveCorrectOrderingCompare() {
        TramDate date = TestEnv.testDay();

        LocationSet destinationStations = new LocationSet();
        final Station manchesterAirport = stationRepository.getStationById(ManAirport.getId());
        destinationStations.add(manchesterAirport);
//        LatLong destinationLatLon = nearPiccGardens.latLong();

        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        LowestCostsForDestRoutes lowestCostForRoutes = routeToRouteCosts.getLowestCostCalcutatorFor(destinationStations,
                date, timeRange, TramsOnly);

        TraversalOps traversalOpsForDest = new TraversalOps(txn, nodeOperations, tripRepository,
                destinationStations, lowestCostForRoutes, date);

        Station altrincham = stationRepository.getStationById(TramStations.Altrincham.getId());

        HasId<Route> pickupAtAlty = altrincham.getPickupRoutes().iterator().next();
        Route vicToAirport = manchesterAirport.getDropoffRoutes().iterator().next();

        assertEquals(0, traversalOpsForDest.onDestRouteFirst(vicToAirport, vicToAirport));
        assertEquals(-1, traversalOpsForDest.onDestRouteFirst(vicToAirport, pickupAtAlty), "wrong for " + vicToAirport.getId() + " " + pickupAtAlty.getId());
        assertEquals(+1, traversalOpsForDest.onDestRouteFirst(pickupAtAlty, vicToAirport));
        assertEquals(0, traversalOpsForDest.onDestRouteFirst(pickupAtAlty, pickupAtAlty));

    }

}
