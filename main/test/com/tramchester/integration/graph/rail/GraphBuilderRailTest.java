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
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.ManchesterPiccadilly;
import static com.tramchester.integration.testSupport.rail.RailStationIds.Stockport;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
class GraphBuilderRailTest {
    private static ComponentContainer componentContainer;

    private GraphTransactionNeo4J txn;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        // TODO chance to GM bounds only?
        TramchesterConfig testConfig = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.GreaterManchester);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
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
                    GraphNode found = txn.findNode(routeStation);
                    assertNotNull(found, routeStation.toString());
        });
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {

        Station piccadilly = ManchesterPiccadilly.from(transportData);
        GraphNode startNode = txn.findNode(piccadilly);
        List<ImmutableGraphRelationship> outboundLinks = startNode.getRelationships(txn, GraphDirection.Outgoing, LINKED).toList();

        assertEquals(25, outboundLinks.size(), outboundLinks.toString());

        //return getStationIdFrom(node.getNode());
        Set<IdFor<Station>> destinations = outboundLinks.stream().
                map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphNode::getStationId).
                collect(Collectors.toSet());

        assertTrue(destinations.contains(Stockport.getId()), destinations.toString());
    }

    @Test
    void shouldHaveCorrectPlatformCosts() {
        Station piccadilly = ManchesterPiccadilly.from(transportData);
        Duration cost = piccadilly.getMinChangeDuration();
        Set<Platform> platforms = piccadilly.getPlatforms();

        assertFalse(platforms.isEmpty());

        platforms.forEach(platform -> {
            GraphNode node = txn.findNode(platform);
            GraphRelationship leave = node.getSingleRelationship(txn, TransportRelationshipTypes.LEAVE_PLATFORM, GraphDirection.Outgoing);
            Duration leaveCost = leave.getCost(); // GraphProps.getCost(leave);
            assertEquals(Duration.ZERO, leaveCost, "leave cost wrong for " + platform);

            GraphRelationship enter = node.getSingleRelationship(txn, TransportRelationshipTypes.ENTER_PLATFORM, GraphDirection.Incoming);
            Duration enterCost = enter.getCost(); // GraphProps.getCost(enter);
            assertEquals(cost, enterCost, "wrong enter cost for " + platform.getId());
        });

        platforms.forEach(platform -> {
            GraphNode node = txn.findNode(platform);
            if (node.hasRelationship(GraphDirection.Outgoing, BOARD)) {
                GraphRelationship board = node.getSingleRelationship(txn, BOARD, GraphDirection.Outgoing);
                Duration boardCost = board.getCost(); // GraphProps.getCost(board);
                assertEquals(Duration.ZERO, boardCost, "board cost wrong for " + platform);
            }

            if (node.hasRelationship(GraphDirection.Incoming, DEPART)) {
                GraphRelationship depart = node.getSingleRelationship(txn, DEPART, GraphDirection.Incoming);
                Duration enterCost = depart.getCost(); //GraphProps.getCost(depart);
                assertEquals(Duration.ZERO, enterCost, "depart wrong cost for " + platform.getId());
            }
        });
    }

    @Test
    void shouldHaveCorrectRouteRelationshipsStockportToManchesterPiccadilly() {
        Station stockport = Stockport.from(transportData);

        final Set<GraphNode> stockportRouteStations = getRouteStationNodes(stockport);
        assertFalse(stockportRouteStations.isEmpty());

        Station manPic = ManchesterPiccadilly.from(transportData);
        Set<GraphNode> manPicRouteStations = getRouteStationNodes(manPic);

        Stream<GraphRelationship> outgoingFromStockport = stockportRouteStations.stream().
                flatMap(node -> node.getRelationships(txn, GraphDirection.Outgoing, ON_ROUTE));

        List<GraphRelationship> endIsManPic = outgoingFromStockport.
                filter(relationship -> manPicRouteStations.contains(relationship.getEndNode(txn))).
                toList();

        assertFalse(endIsManPic.isEmpty(), outgoingFromStockport.toString());

        final Duration limit = Duration.ofMinutes(5);
        endIsManPic.forEach(
                relationship -> assertTrue(relationship.getCost().compareTo(limit) > 0,
                        relationship.getAllProperties().toString()));
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
