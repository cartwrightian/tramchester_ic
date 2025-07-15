package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationGroupsGraphBuilder;
import com.tramchester.integration.testSupport.config.IntegrationTramBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.TramBusTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.TransportRelationshipTypes.NEIGHBOUR;
import static com.tramchester.graph.graphbuild.GraphLabel.STATION;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@TramBusTest
class AddNeighboursGraphBuilderTest {

    private static GraphDatabase graphDatabase;
    private static StationRepository stationRepository;
    private static StationGroupsRepository stationGroupRepository;
    private StationLocalityGroup shudehillCentralBus;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private GraphTransaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramBusTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        graphDatabase = componentContainer.get(GraphDatabaseNeo4J.class);

        // make sure composites added to the DB
        StationGroupsGraphBuilder builder = componentContainer.get(StationGroupsGraphBuilder.class);
        builder.getReady();

        stationRepository = componentContainer.get(StationRepository.class);
        stationGroupRepository = componentContainer.get(StationGroupsRepository.class);

        // force init of main DB and hence save of VERSION node, so avoid multiple rebuilds of the DB
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {

        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        shudehillCentralBus = KnownLocality.Shudehill.from(stationGroupRepository);
        shudehillTram = stationRepository.getStationById(Shudehill.getId());

        // force init of main DB and hence save of VERSION node, so avoid multiple rebuilds of the DB
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);

        txn = graphDatabase.beginTx();
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldFindTheCompositeBusStation() {
        assertNotNull(shudehillCentralBus);

        GraphNode compNode = txn.findNode(shudehillCentralBus);
        assertNotNull(compNode, "No node found for " + compNode);
        shudehillCentralBus.getAllContained().stream().forEach(busStop -> {
            GraphNode busNode = txn.findNode(busStop);
            assertNotNull(busNode, "No node found for " + busStop);
        });
    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromTram() {
        GraphNode tramNode = txn.findNode(shudehillTram);
        assertNotNull(tramNode);

        Station victoria = TramStations.Victoria.fake();

        Set<GraphRelationship> outFromTram = getRelationships(tramNode, OUTGOING);
        Set<GraphRelationship> towardsTram = getRelationships(tramNode, INCOMING);

        assertFalse(seenNode(txn, victoria, outFromTram, this::getEndNode));
        assertFalse(seenNode(txn, shudehillTram, outFromTram, this::getEndNode));
        assertFalse(seenNode(txn, victoria, towardsTram, this::getStartNode));

        shudehillCentralBus.getAllContained().stream().forEach(busStop -> {
            assertTrue(seenNode(txn, busStop, outFromTram, this::getEndNode));
            assertTrue(seenNode(txn, busStop, towardsTram, this::getStartNode));
        });

    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromBus() {

        shudehillCentralBus.getAllContained().stream().forEach(busStop -> {
            GraphNode busNode = txn.findNode(busStop);
            assertNotNull(busNode, "No node found for " + busStop);

            Set<GraphRelationship> awayFrom = getRelationships(busNode, OUTGOING);
            assertTrue(seenNode(txn, shudehillTram, awayFrom, this::getEndNode));

            Set<GraphRelationship> towards = getRelationships(busNode, INCOMING);
            assertTrue(seenNode(txn, shudehillTram, towards, this::getStartNode));
        });

    }

    @Test
    void shouldHaveExpectedNumberForTramStations() {
        assertEquals(NUM_TFGM_TRAM_STATIONS, countStationNodes(GraphLabel.TRAM));
    }

    @Test
    void shouldHaveNodesForAllStations() {
        final Set<Station> stations = stationRepository.getStationsServing(Tram);
        long tram = stations.size();
        assertEquals(NUM_TFGM_TRAM_STATIONS, tram);

        stations.forEach(station ->
                assertNotNull(txn.findNode(station), station.getId() + " is missing from DB"));
    }

    @Test
    void shouldHaveExpectedNumbersForBusStations() {

        long busStations = stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Bus);

        assertEquals(busStations, countStationNodes(GraphLabel.BUS));
    }

    private long countStationNodes(GraphLabel graphLabel) {
        Stream<ImmutableGraphNode> stationNodes = txn.findNodes(STATION); // graphDatabase.findNodes(txn, STATION);
        return stationNodes.filter(node -> node.hasLabel(graphLabel)).count();
    }

    private GraphNode getStartNode(GraphRelationship graphRelationship) {
        return graphRelationship.getStartNode(txn);
    }

    private GraphNode getEndNode(GraphRelationship graphRelationship) {
        return graphRelationship.getEndNode(txn);
    }

    private Set<GraphRelationship> getRelationships(GraphNode node, Direction direction) {
        return node.getRelationships(txn, direction, NEIGHBOUR).collect(Collectors.toSet());
    }

    private boolean seenNode(GraphTransaction txn, Station station, Set<GraphRelationship> relationships, SelectNode selectNode) {
        GraphNode nodeToFind = txn.findNode(station);
        assertNotNull(nodeToFind, "no node found for " + station);

        boolean seenNode = false;
        for (GraphRelationship relationship : relationships) {
            if (nodeFrom(selectNode, relationship).equals(nodeToFind)) {
                seenNode = true;
                break;
            }
        }
        return seenNode;
    }

    private GraphNode nodeFrom(SelectNode selectNode, GraphRelationship relationship) {
        return selectNode.getNode(relationship);
    }

    private interface SelectNode {
        GraphNode getNode(GraphRelationship relationship);
    }

}
