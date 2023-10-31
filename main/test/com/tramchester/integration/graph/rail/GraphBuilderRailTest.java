package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.*;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
class GraphBuilderRailTest {
    private static ComponentContainer componentContainer;

    private GraphTransaction txn;
    private GraphQuery graphQuery;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        graphQuery = componentContainer.get(GraphQuery.class);
        GraphDatabase service = componentContainer.get(GraphDatabase.class);
        transportData = componentContainer.get(TransportData.class);

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();

        txn = service.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        if (txn!=null) {
            txn.close();
        }
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveOneNodePerRouteStation() {
        // getRouteStationNode will throw if multiple nodes are found
        transportData.getRouteStations().stream().
                filter(RouteStation::isActive).
                forEach(routeStation -> {
                    GraphNode found = graphQuery.getRouteStationNode(txn, routeStation);
                    assertNotNull(found, routeStation.toString());
        });
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {

        Station piccadilly = ManchesterPiccadilly.from(transportData);
        GraphNode startNode = graphQuery.getStationNode(txn, piccadilly);
        List<GraphRelationship> outboundLinks = startNode.getRelationships(txn, Direction.OUTGOING, LINKED).toList();

        assertEquals(35, outboundLinks.size(), outboundLinks.toString());

        Set<IdFor<Station>> destinations = outboundLinks.stream().map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(Station.createId("STKP")), destinations.toString());
    }

    @Test
    void shouldHaveCorrectPlatformCosts() {
        Station piccadilly = ManchesterPiccadilly.from(transportData);
        Duration cost = piccadilly.getMinChangeDuration();
        Set<Platform> platforms = piccadilly.getPlatforms();

        assertFalse(platforms.isEmpty());

        platforms.forEach(platform -> {
            GraphNode node = graphQuery.getPlatformNode(txn, platform);
            Relationship leave = node.getSingleRelationship(TransportRelationshipTypes.LEAVE_PLATFORM, Direction.OUTGOING);
            Duration leaveCost = GraphProps.getCost(leave);
            assertEquals(Duration.ZERO, leaveCost, "leave cost wrong for " + platform);

            Relationship enter = node.getSingleRelationship(TransportRelationshipTypes.ENTER_PLATFORM, Direction.INCOMING);
            Duration enterCost = GraphProps.getCost(enter);
            assertEquals(cost, enterCost, "wrong enter cost for " + platform.getId());
        });

        platforms.forEach(platform -> {
            GraphNode node = graphQuery.getPlatformNode(txn, platform);
            if (node.hasRelationship(Direction.OUTGOING, BOARD)) {
                Relationship board = node.getSingleRelationship(BOARD, Direction.OUTGOING);
                Duration boardCost = GraphProps.getCost(board);
                assertEquals(Duration.ZERO, boardCost, "board cost wrong for " + platform);
            }

            if (node.hasRelationship(Direction.INCOMING, DEPART)) {
                Relationship depart = node.getSingleRelationship(DEPART, Direction.INCOMING);
                Duration enterCost = GraphProps.getCost(depart);
                assertEquals(Duration.ZERO, enterCost, "depart wrong cost for " + platform.getId());
            }
        });
    }

    @Test
    void shouldHaveCorrectRouteRelationshipsCreweToMKC() {
        Station miltonKeynes = MiltonKeynesCentral.from(transportData);
        final Set<GraphNode> routeStationNodes = getRouteStationNodes(miltonKeynes);
        assertFalse(routeStationNodes.isEmpty());

        //Set<GraphNode> mkNodeIds = new HashSet<>(routeStationNodes);

        Station crewe = Crewe.from(transportData);
        Set<GraphNode> creweRouteStationsNodes = getRouteStationNodes(crewe);

        Set<GraphRelationship> outgoingFromCrewe = creweRouteStationsNodes.stream().
                flatMap(node -> node.getRelationships(txn, Direction.OUTGOING, ON_ROUTE)).
                collect(Collectors.toSet());

        List<GraphRelationship> endIsMKC = outgoingFromCrewe.stream().
                filter(relationship -> routeStationNodes.contains(relationship.getEndNode(txn))). // GraphNode.fromEnd(relationship))).
                toList();

        assertFalse(endIsMKC.isEmpty(), outgoingFromCrewe.toString());

        final Duration tenMins = Duration.ofMinutes(10);
        endIsMKC.forEach(relationship -> assertTrue(
                GraphProps.getCost(relationship).compareTo(tenMins) > 0, relationship.getAllProperties().toString()));
    }

    @NotNull
    private Set<GraphNode> getRouteStationNodes(Station station) {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(station.getId());
        return routeStations.stream().
                map(routeStation -> graphQuery.getRouteStationNode(txn, routeStation)).
                filter(Objects::nonNull).
                collect(Collectors.toSet());
    }


}
