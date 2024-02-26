package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class TraversalOpsTest {
    private static ComponentContainer componentContainer;

    private NodeContentsRepository nodeOperations;
    private TripRepository tripRepository;
    private StationRepository stationRepository;
    private MutableGraphTransaction txn;

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
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTxMutable();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

//    @Test
//    void shouldHaveCorrectOrderingCompare() {
//        TramDate date = TestEnv.testDay();
//
//        LocationSet<Station> destinationStations = new LocationSet<>();
//        final Station manchesterAirport = stationRepository.getStationById(ManAirport.getId());
//        destinationStations.add(manchesterAirport);
//
//        TraversalOps traversalOpsForDest = new TraversalOps(txn, nodeOperations, tripRepository,
//                destinationStations, date, TramTime.of(9,45));
//
//        Station altrincham = stationRepository.getStationById(TramStations.Altrincham.getId());
//
//        HasId<Route> pickupAtAlty = altrincham.getPickupRoutes().iterator().next();
//        Route vicToAirport = manchesterAirport.getDropoffRoutes().iterator().next();
//
//        assertEquals(0, traversalOpsForDest.onDestRouteFirst(vicToAirport, vicToAirport));
//        assertEquals(-1, traversalOpsForDest.onDestRouteFirst(vicToAirport, pickupAtAlty), "wrong for " + vicToAirport.getId() + " " + pickupAtAlty.getId());
//        assertEquals(+1, traversalOpsForDest.onDestRouteFirst(pickupAtAlty, vicToAirport));
//        assertEquals(0, traversalOpsForDest.onDestRouteFirst(pickupAtAlty, pickupAtAlty));
//
//    }

}
