package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.inMemory.Graph;
import com.tramchester.graph.core.inMemory.GraphDatabaseInMemory;
import com.tramchester.graph.core.inMemory.NodeIdInMemory;
import com.tramchester.graph.core.inMemory.SaveGraph;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.KnownTramRouteEnum;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.TransportRelationshipTypes.TO_SERVICE;
import static com.tramchester.unit.graph.inMemory.GraphSaveAndLoadTest.CreateGraphDatabaseInMemory;
import static com.tramchester.testSupport.reference.TramStations.VeloPark;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("WIP")
public class MissingTripListOnServicesTest {

    private static GuiceContainerDependencies componentContainer;
    private RouteStationId routeStationId;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig(GraphDBType.InMemory);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        TramDate when = TestEnv.testDay();

        KnownTramRouteEnum route = KnownTramRoute.getYellow(when);

        routeStationId = RouteStationId.createId(route.getId(), VeloPark.getId());

        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldNotHaveMissingTripsOnAnyServiceRelations() {

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            Stream<GraphNode> nodes = txn.findNodes(GraphLabel.ROUTE_STATION);

            List<GraphNode> haveMissingTrips = nodes.
                    filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            anyMatch(rel -> rel.getTripIds().isEmpty())).toList();

            assertTrue(haveMissingTrips.isEmpty());
        }
    }

    @Test
    void shouldNotHaveMissingTripsOnAnyServiceRelationsProblemGraph() {

        Graph problemGraph = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_FAIL);

        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(problemGraph, componentContainer);
        graphDatabase.start();

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            Stream<GraphNode> nodes = txn.findNodes(GraphLabel.ROUTE_STATION);

            List<GraphNode> haveMissingTrips = nodes.
                    filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            anyMatch(rel -> rel.getTripIds().isEmpty())).toList();

            assertEquals(0,haveMissingTrips.size());
        }
    }

    @Test
    void shouldNotHaveMissingTripsOnAnyServiceRelationsProblemGraphAnyWithAllMissing() {

        Graph problemGraph = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_FAIL);

        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(problemGraph, componentContainer);
        graphDatabase.start();

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            Stream<GraphNode> nodes = txn.findNodes(GraphLabel.ROUTE_STATION);

            List<GraphNode> anyMissingTrips = nodes.
                    filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            anyMatch(rel -> rel.getTripIds().isEmpty())).toList();

            List<GraphNode> allMissingTrips = anyMissingTrips.
                    stream().filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            allMatch(rel -> rel.getTripIds().isEmpty())).toList();

            assertEquals(0,allMissingTrips.size());
        }
    }

    @Test
    void shouldNotReproIssueWithMissingTripsOnToServiceRelationships() {

        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            GraphNode problemNode = txn.findNode(routeStation);

            List<GraphRelationship> toService = problemNode.getRelationships(txn, Outgoing, TO_SERVICE).toList();
            assertFalse(toService.isEmpty());

            List<GraphRelationship> noTrips = toService.stream().
                    filter(relationship -> relationship.getTripIds().isEmpty()).toList();

            assertEquals(0, noTrips.size());
        }
    }

    @Test
    void shouldReproIssueWithMissingTripsOnToServiceRelationships() {

        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        Graph problemGraph = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_FAIL);

        GraphDatabaseInMemory graphDatabase = CreateGraphDatabaseInMemory(problemGraph, componentContainer);
        graphDatabase.start();

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            GraphNode problemNode = txn.findNode(routeStation);

            // todo this will break but that's ok, just here to double check have reproduced the exact issue
            NodeIdInMemory nodeId = new NodeIdInMemory(430);
            assertEquals(nodeId, problemNode.getId());

            List<GraphRelationship> toService = problemNode.getRelationships(txn, Outgoing, TO_SERVICE).toList();
            assertFalse(toService.isEmpty());

            List<GraphRelationship> noTrips = toService.stream().
                    filter(relationship -> relationship.getTripIds().isEmpty()).toList();

            assertNotEquals(toService.size(), noTrips.size());
        }
    }
}
