package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
class GraphBuilderRailTest {
    private static ComponentContainer componentContainer;

    private MutableGraphTransaction txn;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        // TODO chance to GM bounds only?
        TramchesterConfig testConfig = new IntegrationRailTestConfig(true);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase service = componentContainer.get(GraphDatabase.class);
        transportData = componentContainer.get(TransportData.class);

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();

        txn = service.beginTxMutable();
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
                    GraphNode found = txn.findNode(routeStation);
                    assertNotNull(found, routeStation.toString());
        });
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {

        Station piccadilly = ManchesterPiccadilly.from(transportData);
        GraphNode startNode = txn.findNode(piccadilly);
        List<ImmutableGraphRelationship> outboundLinks = startNode.getRelationships(txn, Direction.OUTGOING, LINKED).toList();

        assertEquals(35, outboundLinks.size(), outboundLinks.toString());

        Set<IdFor<Station>> destinations = outboundLinks.stream().map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(node -> {
                    return node.getStationId();
                    //return getStationIdFrom(node.getNode());
                }).collect(Collectors.toSet());

        assertTrue(destinations.contains(Station.createId("STKP")), destinations.toString());
    }

    @Test
    void shouldHaveCorrectPlatformCosts() {
        Station piccadilly = ManchesterPiccadilly.from(transportData);
        Duration cost = piccadilly.getMinChangeDuration();
        Set<Platform> platforms = piccadilly.getPlatforms();

        assertFalse(platforms.isEmpty());

        platforms.forEach(platform -> {
            GraphNode node = txn.findNode(platform);
            GraphRelationship leave = node.getSingleRelationship(txn, TransportRelationshipTypes.LEAVE_PLATFORM, Direction.OUTGOING);
            Duration leaveCost = leave.getCost(); // GraphProps.getCost(leave);
            assertEquals(Duration.ZERO, leaveCost, "leave cost wrong for " + platform);

            GraphRelationship enter = node.getSingleRelationship(txn, TransportRelationshipTypes.ENTER_PLATFORM, Direction.INCOMING);
            Duration enterCost = enter.getCost(); // GraphProps.getCost(enter);
            assertEquals(cost, enterCost, "wrong enter cost for " + platform.getId());
        });

        platforms.forEach(platform -> {
            GraphNode node = txn.findNode(platform);
            if (node.hasRelationship(Direction.OUTGOING, BOARD)) {
                GraphRelationship board = node.getSingleRelationship(txn, BOARD, Direction.OUTGOING);
                Duration boardCost = board.getCost(); // GraphProps.getCost(board);
                assertEquals(Duration.ZERO, boardCost, "board cost wrong for " + platform);
            }

            if (node.hasRelationship(Direction.INCOMING, DEPART)) {
                GraphRelationship depart = node.getSingleRelationship(txn, DEPART, Direction.INCOMING);
                Duration enterCost = depart.getCost(); //GraphProps.getCost(depart);
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

        Stream<GraphRelationship> outgoingFromCrewe = creweRouteStationsNodes.
                stream().flatMap(node -> node.getRelationships(txn, Direction.OUTGOING, ON_ROUTE));

        List<GraphRelationship> endIsMKC = outgoingFromCrewe.
                filter(relationship -> routeStationNodes.contains(relationship.getEndNode(txn))). // GraphNode.fromEnd(relationship))).
                toList();

        assertFalse(endIsMKC.isEmpty(), outgoingFromCrewe.toString());

        final Duration tenMins = Duration.ofMinutes(10);
        endIsMKC.forEach(relationship -> assertTrue(
                relationship.getCost().compareTo(tenMins) > 0, relationship.getAllProperties().toString()));
    }

    @NotNull
    private Set<GraphNode> getRouteStationNodes(Station station) {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(station.getId());
        return routeStations.stream().
                map(routeStation -> txn.findNode(routeStation)).
                filter(Objects::nonNull).
                collect(Collectors.toSet());
    }


}
