package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationGroupsGraphBuilder;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.NEIGHBOUR;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@BusTest
class AddNeighboursGraphBuilderTest {

    private static GraphDatabase graphDatabase;
    private StationGroup shudehillCentralBus;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private MutableGraphTransaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        graphDatabase = componentContainer.get(GraphDatabase.class);

        // make sure composites added to the DB
        StationGroupsGraphBuilder builder = componentContainer.get(StationGroupsGraphBuilder.class);
        builder.getReady();

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

        BusStations.CentralStops centralStops = new BusStations.CentralStops(componentContainer);

        shudehillCentralBus = centralStops.Shudehill();
        shudehillTram = stationRepository.getStationById(Shudehill.getId());

        // force init of main DB and hence save of VERSION node, so avoid multiple rebuilds of the DB
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);

        txn = graphDatabase.beginTxMutable();
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
        shudehillCentralBus.getContained().forEach(busStop -> {
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

        shudehillCentralBus.getContained().forEach(busStop -> {
            assertTrue(seenNode(txn, busStop, outFromTram, this::getEndNode));
            assertTrue(seenNode(txn, busStop, towardsTram, this::getStartNode));
        });

    }

    private GraphNode getStartNode(GraphRelationship graphRelationship) {
        return graphRelationship.getStartNode(txn);
    }

    private GraphNode getEndNode(GraphRelationship graphRelationship) {
        return graphRelationship.getEndNode(txn);
    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromBus() {

        shudehillCentralBus.getContained().forEach(busStop -> {
            GraphNode busNode = txn.findNode(busStop);
            assertNotNull(busNode, "No node found for " + busStop);

            Set<GraphRelationship> awayFrom = getRelationships(busNode, OUTGOING);
            assertTrue(seenNode(txn, shudehillTram, awayFrom, this::getEndNode));

            Set<GraphRelationship> towards = getRelationships(busNode, INCOMING);
            assertTrue(seenNode(txn, shudehillTram, towards, this::getStartNode));
        });

    }

    private Set<GraphRelationship> getRelationships(GraphNode node, Direction direction) {
        return node.getRelationships(txn, direction, NEIGHBOUR).collect(Collectors.toSet());
    }

    private boolean seenNode(MutableGraphTransaction txn, Station station, Set<GraphRelationship> relationships, SelectNode selectNode) {
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
