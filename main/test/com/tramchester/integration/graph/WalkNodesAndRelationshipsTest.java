package com.tramchester.integration.graph;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.LocationJourneyPlanner;
import com.tramchester.graph.search.WalkNodesAndRelationships;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.core.GraphDirection.Incoming;
import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.TransportRelationshipTypes.WALKS_FROM_STATION;
import static com.tramchester.graph.reference.TransportRelationshipTypes.WALKS_TO_STATION;
import static com.tramchester.testSupport.reference.KnownLocations.nearStPetersSquare;
import static com.tramchester.testSupport.reference.TramStations.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalkNodesAndRelationshipsTest {
    private static GuiceContainerDependencies componentContainer;
    private static IntegrationTramTestConfig testConfig;
    ;
    private MutableGraphTransaction txn;
    private WalkNodesAndRelationships walkNodesAndRelationships;
    private JourneyRequest journeyRequest;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        TramDuration maxJourneyDuration = TramDuration.ofMinutes(testConfig.getMaxJourneyDuration());

        journeyRequest = new JourneyRequest(TestEnv.testDay(), TramTime.of(9, 0), false,
                0, maxJourneyDuration, 1, EnumSet.of(TransportMode.Tram));

        // init graph build
        componentContainer.get(LocationJourneyPlanner.class);

        txn = database.beginTxMutable();

        walkNodesAndRelationships = new WalkNodesAndRelationships(txn);

    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldCreateAndThenDeleteWalkingNode() {

        KnownLocations knownLocation = nearStPetersSquare;

        assertEquals(0,txn.findNodes(GraphLabel.QUERY_NODE).count());

        walkNodesAndRelationships.createWalkingNode(knownLocation.location(), journeyRequest);

        List<GraphNode> nodes = txn.findNodes(GraphLabel.QUERY_NODE).toList();

        assertEquals(1, nodes.size());

        GraphNode node = nodes.getFirst();

        assertEquals(knownLocation.latLong(), node.getLatLong());

        assertEquals(format("%s_%s", knownLocation.location().getLatLong(), journeyRequest.getUid().toString()), node.getWalkId());

        walkNodesAndRelationships.delete();

        assertEquals(0,txn.findNodes(GraphLabel.QUERY_NODE).count());

    }

    @Test
    void shouldCreateWalkToStartStationAndThenDelete() {

        KnownLocations knownLocation = nearStPetersSquare;
        Station startStation = TramStations.StPetersSquare.fake();
        TramDuration cost = TramDuration.ofSeconds(75);

        StationWalk walk = new StationWalk(startStation, cost);
        Set<StationWalk> stationWalks = Collections.singleton(walk);

        assertEquals(0,txn.findNodes(GraphLabel.QUERY_NODE).count());

        MutableGraphNode queryNode = walkNodesAndRelationships.createWalkingNode(knownLocation.location(), journeyRequest);
        GraphNode stationNode = txn.findNode(startStation);

        walkNodesAndRelationships.createWalksToStart(queryNode, stationWalks);

        assertEquals(1, txn.findRelationships(WALKS_TO_STATION).count());

        List<GraphRelationship> relationships = queryNode.getRelationships(txn, Outgoing, WALKS_TO_STATION)
                .toList();

        assertEquals(1, relationships.size());

        GraphRelationship relationship = relationships.getFirst();

        assertEquals(cost, relationship.getCost());
        assertEquals(queryNode.getId(), relationship.getStartNodeId(txn));
        assertEquals(stationNode.getId(), relationship.getEndNodeId(txn));
        assertEquals(startStation.getId(), relationship.getStationId());

        walkNodesAndRelationships.delete();

        assertEquals(0, txn.findNodes(GraphLabel.QUERY_NODE).count());
        assertEquals(0, txn.findRelationships(WALKS_TO_STATION).count());
    }


    @Test
    void shouldCreateMultipleWalksToStartStationAndThenDelete() {

        Set<StationWalk> stationWalks = createStationWalks();

        assertEquals(0, txn.findNodes(GraphLabel.QUERY_NODE).count());

        MutableGraphNode queryNode = walkNodesAndRelationships.createWalkingNode(nearStPetersSquare.location(), journeyRequest);

        walkNodesAndRelationships.createWalksToStart(queryNode, stationWalks);

        assertEquals(stationWalks.size(), txn.findRelationships(WALKS_TO_STATION).count());

        List<GraphRelationship> relationships = queryNode.getRelationships(txn, Outgoing, WALKS_TO_STATION)
                .toList();

        assertEquals(stationWalks.size(), relationships.size());

        Map<IdFor<Station>, GraphRelationship> results = relationships.stream().collect(Collectors.toMap(GraphRelationship::getStationId, rel->rel));

        for (StationWalk expected : stationWalks) {
            Station expectedStation = expected.getStation();
            GraphNode stationNode = txn.findNode(expectedStation);

            assertTrue(results.containsKey(expectedStation.getId()));

            GraphRelationship graphRelationship = results.get(expectedStation.getId());
            assertEquals(queryNode.getId(), graphRelationship.getStartNodeId(txn));
            assertEquals(stationNode, graphRelationship.getEndNode(txn));
            assertEquals(graphRelationship.getCost(), expected.getCost());
        }

        walkNodesAndRelationships.delete();

        assertEquals(0, txn.findNodes(GraphLabel.QUERY_NODE).count());
        assertEquals(0, txn.findRelationships(WALKS_TO_STATION).count());
    }

    @Test
    void shouldCreateWalkFromStationAndThenDelete() {

        Station startStation = TramStations.StPetersSquare.fake();
        TramDuration cost = TramDuration.ofSeconds(75);

        StationWalk walk = new StationWalk(startStation, cost);
        Set<StationWalk> stationWalks = Collections.singleton(walk);

        assertEquals(0,txn.findNodes(GraphLabel.QUERY_NODE).count());

        MutableGraphNode queryNode = walkNodesAndRelationships.createWalkingNode(nearStPetersSquare.location(), journeyRequest);
        GraphNode stationNode = txn.findNode(startStation);

        walkNodesAndRelationships.createWalksToDest(queryNode, stationWalks);

        assertEquals(1, txn.findRelationships(WALKS_FROM_STATION).count());

        List<GraphRelationship> relationships = stationNode.getRelationships(txn, Outgoing, WALKS_FROM_STATION)
                .toList();

        assertEquals(1, relationships.size());

        GraphRelationship relationship = relationships.getFirst();

        assertEquals(cost, relationship.getCost());
        assertEquals(stationNode.getId(), relationship.getStartNodeId(txn));
        assertEquals(queryNode.getId(), relationship.getEndNodeId(txn));
        assertEquals(startStation.getId(), relationship.getStationId());

        walkNodesAndRelationships.delete();

        assertEquals(0, txn.findNodes(GraphLabel.QUERY_NODE).count());
        assertEquals(0, txn.findRelationships(WALKS_FROM_STATION).count());
    }

    @Test
    void shouldCreateMultipleWalksFromStationsAndThenDelete() {

        Set<StationWalk> stationWalks = createStationWalks();

        assertEquals(0, txn.findNodes(GraphLabel.QUERY_NODE).count());

        MutableGraphNode queryNode = walkNodesAndRelationships.createWalkingNode(nearStPetersSquare.location(), journeyRequest);

        walkNodesAndRelationships.createWalksToDest(queryNode, stationWalks);

        assertEquals(stationWalks.size(), txn.findRelationships(WALKS_FROM_STATION).count());

        List<GraphRelationship> relationships = queryNode.getRelationships(txn, Incoming, WALKS_FROM_STATION)
                .toList();

        assertEquals(stationWalks.size(), relationships.size());

        Map<IdFor<Station>, GraphRelationship> results = relationships.stream().collect(Collectors.toMap(GraphRelationship::getStationId, rel->rel));

        for (StationWalk expected : stationWalks) {
            Station expectedStation = expected.getStation();
            GraphNode stationNode = txn.findNode(expectedStation);

            assertTrue(results.containsKey(expectedStation.getId()));
            GraphRelationship graphRelationship = results.get(expectedStation.getId());

            assertEquals(stationNode, graphRelationship.getStartNode(txn));
            assertEquals(queryNode.getId(), graphRelationship.getEndNodeId(txn));
            assertEquals(graphRelationship.getCost(), expected.getCost());
        }

        walkNodesAndRelationships.delete();

        assertEquals(0, txn.findNodes(GraphLabel.QUERY_NODE).count());
        assertEquals(0, txn.findRelationships(WALKS_FROM_STATION).count());
    }

    private @NotNull HashSet<StationWalk> createStationWalks() {
        return new HashSet<>(Arrays.asList(
                walk(MarketStreet, 3),
                walk(PiccadillyGardens, 4),
                walk(StPetersSquare, 5)));
    }

    StationWalk walk(TramStations station, int costInMins) {
        return new StationWalk(station.fake(), TramDuration.ofMinutes(costInMins));
    }


}
