package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.PlatformStationState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.TraffordBar;

public class TraversalStateTest {
    private static GuiceContainerDependencies componentContainer;
    private TraversalStateFactory traversalStateFactory;
    private MutableGraphTransaction txn;
    private NodeContentsRepository nodeContentsRepository;
    private TripRepository tripRepository;
    private StationRepository stationRepository;

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
    void beforeEachOfTheTestsRun() {
        componentContainer.get(TraversalStateFactory.class); // trigger population
        traversalStateFactory = componentContainer.get(TraversalStateFactory.class);
        nodeContentsRepository = componentContainer.get(NodeContentsRepository.class);
        tripRepository = componentContainer.get(TripRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTxMutable();
    }

    @AfterEach
    void onceAfterEachTest() {
        txn.close();
    }

    @Disabled("WIP")
    @Test
    void SPIKE_shouldBeAbleToTest() {
        PlatformStationState.Builder builder = traversalStateFactory.getTowardsStation(TraversalStateType.PlatformState);

        TramDate date = TestEnv.testDay();
        TramTime time = TramTime.of(8,42);

        LocationCollection endStations = new LocationSet<Station>();
        final TraversalOps traversalOps = new TraversalOps(txn, nodeContentsRepository, tripRepository, endStations, date,
                time);

        EnumSet<TransportMode> transportModes = EnumSet.of(TransportMode.Tram);

        final NotStartedState notStartedState = new NotStartedState(traversalOps, traversalStateFactory,
                transportModes, null);

        JourneyStateUpdate updateState = new JourneyState(time, notStartedState);

        boolean onDiversion = false;
        boolean alreadyOnDiversion = false;

        Duration cost = Duration.ofMinutes(5);

        Station station = TraffordBar.from(stationRepository);
        GraphNode stationNode = txn.findNode(station);

        PlatformStationState state = builder.fromStart(notStartedState, stationNode, cost, updateState, alreadyOnDiversion, onDiversion, txn);

        List<ImmutableGraphRelationship> results = state.getOutbounds(txn).toList();
    }

}
