package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
class RailAndTramGraphBuilderTest {
    private static ComponentContainer componentContainer;

    private MutableGraphTransaction txn;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();
        txn = graphDatabase.beginTxMutable();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForInterchange() {
        Station cornbrook = Cornbrook.from(stationRepository);
        GraphNode cornbrookNode = txn.findNode(cornbrook);
        Stream<ImmutableGraphRelationship> outboundLinks = cornbrookNode.getRelationships(txn, Direction.OUTGOING, LINKED);

        List<ImmutableGraphRelationship> list = outboundLinks.toList();
        assertEquals(3, list.size());

        Set<IdFor<Station>> destinations = list.stream().
                map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphNode::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(TraffordBar.getId()));
        assertTrue(destinations.contains(Pomona.getId()));
        assertTrue(destinations.contains(Deansgate.getId()));
    }

    @Test
    void shouldHaveCorrectRouteStationToStationRouteCosts() {

        Set<RouteStation> routeStations = stationRepository.getRouteStationsFor(Piccadilly.getId());

        routeStations.forEach(routeStation -> {
            GraphNode node = txn.findNode(routeStation);

            GraphRelationship toStation = node.getSingleRelationship(txn, ROUTE_TO_STATION, Direction.OUTGOING);
            Duration costToStation = toStation.getCost(); // GraphProps.getCost(toStation);
            assertEquals(Duration.ZERO, costToStation, "wrong cost for " + routeStation);

            GraphRelationship fromStation = node.getSingleRelationship(txn, STATION_TO_ROUTE, Direction.INCOMING);
            Duration costFromStation = fromStation.getCost(); // GraphProps.getCost(fromStation);
            Duration expected = routeStation.getStation().getMinChangeDuration();
            assertEquals(expected, costFromStation, "wrong cost for " + routeStation);
        });
    }

    @Test
    void shouldHaveExpectedRelationshipsBetweenTramAndTrainStations() {
        Station altyTram = Altrincham.from(stationRepository);
        Station altyTrain = RailStationIds.Altrincham.from(stationRepository);

        Duration expectedCost = Duration.ofMinutes(1);

        GraphNode altyTramNode = txn.findNode(altyTram);
        GraphNode altyTrainNode = txn.findNode(altyTrain);

        assertNotNull(altyTramNode);
        assertNotNull(altyTrainNode);

        List<ImmutableGraphRelationship> fromTram = altyTramNode.getRelationships(txn, Direction.OUTGOING, NEIGHBOUR).toList();
        assertEquals(1, fromTram.size(), "Wrong number of neighbours " + fromTram);

        GraphRelationship tramNeighbour = fromTram.get(0);
        assertEquals(altyTrainNode, tramNeighbour.getEndNode(txn)); // GraphNode.fromEnd(tramNeighbour));
        assertEquals(expectedCost, tramNeighbour.getCost());

        List<ImmutableGraphRelationship> fromTrain = altyTrainNode.getRelationships(txn, Direction.OUTGOING, NEIGHBOUR).toList();
        assertEquals(1, fromTrain.size(), "Wrong number of neighbours " + fromTram);

        GraphRelationship trainNeighbour = fromTrain.get(0);
        assertEquals(altyTramNode, trainNeighbour.getEndNode(txn)); //GraphNode.fromEnd(trainNeighbour));
        assertEquals(expectedCost, trainNeighbour.getCost());

    }

    @Test
    void shouldHaveOneNodePerRouteStation() {
        Set<RouteStation> routeStations = stationRepository.getRouteStations();

        IdSet<RouteStation> noTramRouteStationNode = routeStations.stream().
                filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Tram)).
                filter(routeStation -> txn.findNode(routeStation) == null).
                collect(IdSet.collector());

        assertTrue(noTramRouteStationNode.isEmpty(), noTramRouteStationNode.toString());

        Set<RouteStation> trainRouteStations = routeStations.stream().
                filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Train)).
                filter(RouteStation::isActive). // rail data has 'passed' stations
                collect(Collectors.toSet());

        IdSet<RouteStation> noTrainRouteStationNode = trainRouteStations.stream().
                filter(routeStation -> txn.findNode(routeStation) == null).
                collect(IdSet.collector());

        int numRouteStations = trainRouteStations.size();
        assertTrue(noTrainRouteStationNode.isEmpty(), "Not empty, num route stations is " + numRouteStations
                + " without nodes is " + noTrainRouteStationNode.size());
    }

    @Test
    void shouldHaveExpectedInterchangesInTheGraph() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        IdSet<Station> fromConfigAndDiscovered = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        Stream<ImmutableGraphNode> interchangeNodes = txn.findNodes(GraphLabel.INTERCHANGE);

        IdSet<Station> fromDB = interchangeNodes.map(GraphNode::getStationId).collect(IdSet.idCollector());

        assertEquals(fromConfigAndDiscovered, fromDB, "Graph clean and rebuild needed?");
    }

}
