package com.tramchester.integration.graph;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.search.InterchangeGraphNodeLookup;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InterchangeGraphNodeLookupTest {

    private static GuiceContainerDependencies componentContainer;
    private MutableGraphTransaction txn;

    private static final int TXN_TIMEOUT = 1;
    private StationRepository stationRepository;
    private InterchangeRepository interchangeRepository;

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
    void shouldAllowAllowSingleModeTransferAllStationsForTramOnlyConfig() {

        InterchangeGraphNodeLookup interchangeGraphLookup = new InterchangeGraphNodeLookup(txn, interchangeRepository, TramsOnly);

        Set<InterchangeStation> allInterchanges = interchangeRepository.getAllInterchanges();

        allInterchanges.forEach(interchangeStation -> {
            Station station = interchangeStation.getStation();

            station.getDropoffRoutes().forEach(route -> {
                RouteStation routeStation = stationRepository.getRouteStation(station, route);
                MutableGraphNode node = txn.findNodeMutable(routeStation);
                assertTrue(interchangeGraphLookup.checkInterchangeAvailableFor(node));
            });
        });




    }
}
