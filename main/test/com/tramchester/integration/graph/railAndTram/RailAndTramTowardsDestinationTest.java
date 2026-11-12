package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.collections.IterableWithEmptyCheck;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.AltrinchamLineTests;
import com.tramchester.testSupport.testTags.GMTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramTowardsDestinationTest {
    private static GuiceContainerDependencies componentContainer;
    private GraphTransaction txn;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
    private TramDate when;

    // NOTE: currently (3/2024) most tram stations are not allocated to a local area in Naptan
    // See StationGroupRepositoryTest

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        final TramchesterConfig config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        //TestEnv.deleteDBIfPresent(config);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        tramRouteHelper = new TramRouteHelper(componentContainer);

        componentContainer.get(StagedTransportGraphBuilder.Ready.class);

        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();

        when = TestEnv.testDay();
    }

    @AfterEach
    void onceAfterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @NotNull
    private GraphNode findRouteStation(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        assertNotNull(routeStation);

        GraphNode node = txn.findNode(routeStation);

        assertNotNull(node);
        return node;
    }

    @AltrinchamLineTests
    @Test
    void shouldFindRelationshipsTowardsDestinationWhenNeighbourMatches() {
        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getGreen(when);

        GraphNode tramNode = findRouteStation(station, route);

        Location<?> railDest = RailStationIds.NavigationRaod.from(stationRepository);
        TowardsDestination towardsDestination = new TowardsDestination(railDest, txn);
        towardsDestination.start();

        IterableWithEmptyCheck<GraphRelationship> towards = towardsDestination.fromRouteStation(tramNode);

        assertFalse(towards.isEmpty());
        List<GraphRelationship> results = towards.stream().toList();

        // 2 platforms
        assertEquals(2, results.size());

        results.forEach(result -> {
            GraphNode endNode = result.getEndNode(txn);

            assertTrue(endNode.getLabels().contains(GraphLabel.PLATFORM));

            assertEquals(station.getId(), endNode.getStationId());
        });

    }

    @AltrinchamLineTests
    @Test
    void shouldFindRelationshipsTowardsDestinationWhenNeighbourMatchesFromStation() {
        Station station = NavigationRoad.from(stationRepository);

        GraphNode tramNode = txn.findNode(station);

        Location<?> railDest = RailStationIds.NavigationRaod.from(stationRepository);
        TowardsDestination towardsDestination = new TowardsDestination(railDest, txn);
        towardsDestination.start();

        IterableWithEmptyCheck<GraphRelationship> towards = towardsDestination.fromStation(tramNode);

        assertFalse(towards.isEmpty());
        List<GraphRelationship> results = towards.stream().toList();

        assertEquals(1, results.size());
    }

    @AltrinchamLineTests
    @Test
    void shouldFindRelationshipsTowardsDestinationWhenNeighbourMatchesFromPlatforms() {
        Station station = NavigationRoad.from(stationRepository);

        List<Platform> platforms = new ArrayList<>(station.getPlatforms());
        assertFalse(platforms.isEmpty());

        Platform platform = platforms.getFirst();

        GraphNode tramNode = txn.findNode(platform);

        Location<?> railDest = RailStationIds.NavigationRaod.from(stationRepository);
        TowardsDestination towardsDestination = new TowardsDestination(railDest, txn);
        towardsDestination.start();

        IterableWithEmptyCheck<GraphRelationship> towards = towardsDestination.fromPlatform(tramNode);

        assertFalse(towards.isEmpty());
        List<GraphRelationship> results = towards.stream().toList();

        assertEquals(1, results.size());
    }

    @AltrinchamLineTests
    @Test
    void shouldFindNoRelationshipsIfNotTowardsDestination() {
        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getGreen(when);

        GraphNode node = findRouteStation(station, route);

        TowardsDestination towardsDestination = new TowardsDestination((Bury.from(stationRepository)), txn);
        towardsDestination.start();

        IterableWithEmptyCheck<GraphRelationship> towards = towardsDestination.fromRouteStation(node);

        assertTrue(towards.isEmpty());

    }


}
