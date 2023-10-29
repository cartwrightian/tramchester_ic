package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.graphbuild.CompositeStationGraphBuilder;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.NEIGHBOUR;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@BusTest
class AddNeighboursGraphBuilderTest {

    private static GraphDatabase graphDatabase;
    private StationGroup shudehillCompositeBus;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private GraphQuery graphQuery;
    private Transaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        graphDatabase = componentContainer.get(GraphDatabase.class);

        // make sure composites added to the DB
        CompositeStationGraphBuilder builder = componentContainer.get(CompositeStationGraphBuilder.class);
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
        StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

        graphQuery = componentContainer.get(GraphQuery.class);

        shudehillCompositeBus = stationGroupsRepository.findByName("Shudehill Interchange");
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
        assertNotNull(shudehillCompositeBus);
        GraphNode compNode = graphQuery.getGroupedNode(txn, shudehillCompositeBus);
        assertNotNull(compNode, "No node found for " + compNode);
        shudehillCompositeBus.getContained().forEach(busStop -> {
            GraphNode busNode = graphQuery.getStationNode(txn, busStop);
            assertNotNull(busNode, "No node found for " + busStop);
        });
    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromTram() {
        GraphNode tramNode = graphQuery.getStationNode(txn, shudehillTram);
        assertNotNull(tramNode);

        Station victoria = TramStations.Victoria.fake();

        Set<Relationship> outFromTram = getRelationships(tramNode, OUTGOING);
        Set<Relationship> towardsTram = getRelationships(tramNode, INCOMING);

        assertFalse(seenNode(txn, victoria, outFromTram, Relationship::getEndNode));
        assertFalse(seenNode(txn, shudehillTram, outFromTram, Relationship::getEndNode));
        assertFalse(seenNode(txn, victoria, towardsTram, Relationship::getStartNode));

        shudehillCompositeBus.getContained().forEach(busStop -> {
            assertTrue(seenNode(txn, busStop, outFromTram, Relationship::getEndNode));
            assertTrue(seenNode(txn, busStop, towardsTram, Relationship::getStartNode));
        });

    }

    @Test
    void shouldHaveExpectedNeighbourRelationshipsToFromBus() {

        shudehillCompositeBus.getContained().forEach(busStop -> {
            GraphNode busNode = graphQuery.getStationNode(txn, busStop);
            assertNotNull(busNode, "No node found for " + busStop);

            Set<Relationship> awayFrom = getRelationships(busNode, OUTGOING);
            assertTrue(seenNode(txn, shudehillTram, awayFrom, Relationship::getEndNode));

            Set<Relationship> towards = getRelationships(busNode, INCOMING);
            assertTrue(seenNode(txn, shudehillTram, towards, Relationship::getStartNode));
        });

    }

    private Set<Relationship> getRelationships(GraphNode node, Direction direction) {
        Set<Relationship> result = new HashSet<>();
        Iterable<Relationship> iter = node.getRelationships(direction, NEIGHBOUR);
        iter.forEach(result::add);
        return result;
    }

    private boolean seenNode(Transaction txn, Station station, Set<Relationship> relationships, SelectNode selectNode) {
        GraphNode nodeToFind = graphQuery.getStationNode(txn, station);
        assertNotNull(nodeToFind, "no node found for " + station);

        boolean seenNode = false;
        for (Relationship relationship : relationships) {
            if (nodeFrom(selectNode, relationship).equals(nodeToFind)) {
                seenNode = true;
                break;
            }
        }
        return seenNode;
    }

    private GraphNode nodeFrom(SelectNode selectNode, Relationship relationship) {
        return GraphNode.from(selectNode.getNode(relationship));
    }

    private interface SelectNode {
        Node getNode(Relationship relationship);
    }

}
