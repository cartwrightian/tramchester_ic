package com.tramchester.unit.graph.inMemory;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.tramchester.graph.core.GraphDirection.*;
import static com.tramchester.graph.reference.GraphLabel.*;
import static com.tramchester.graph.reference.TransportRelationshipTypes.FERRY_GOES_TO;
import static com.tramchester.graph.reference.TransportRelationshipTypes.TRAIN_GOES_TO;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionManagerTest {
    private TransactionManager transactionManager;

    // TODO Check throws after delete including labels

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesNow = new ProvidesLocalNow();
        Graph graph = new Graph();
        transactionManager = new TransactionManager(providesNow, graph);
    }

    @Test
    void shouldCreateTransactionWithExpectedId() {
        try (GraphTransaction graphTransaction = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            int id = graphTransaction.getTransactionId();
            assertEquals(1, id);
        }
    }

    @Test
    void shouldCreateNode() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode node = txn.createNode(FERRY);

            assertTrue(node.isNode());
            assertFalse(node.isRelationship());

            assertTrue(node.getLabels().contains(FERRY));
            assertEquals(1, node.getLabels().size());
            assertTrue(node.hasLabel(FERRY));
            assertFalse(node.hasLabel(TRAIN));

            GraphNodeId id = node.getId();
            assertEquals(new NodeIdInMemory(0), id);
        }
    }

    @Test
    void shouldFindNodesByIdAndLabels() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {

            assertFalse(txn.hasAnyMatching(FERRY));

            MutableGraphNode node = txn.createNode(FERRY);
            GraphNodeId id = node.getId();

            assertTrue(txn.hasAnyMatching(FERRY));

            GraphNode foundById = txn.getNodeById(id);
            assertNotNull(foundById);
            assertEquals(id, foundById.getId());

            MutableGraphNode foundByIdMutable = txn.getNodeByIdMutable(id);
            assertNotNull(foundByIdMutable);
            assertEquals(id, foundByIdMutable.getId());

            List<GraphNode> foundByLabel = txn.findNodes(FERRY).toList();
            assertEquals(1, foundByLabel.size());
            assertEquals(id, foundByLabel.getFirst().getId());

            assertEquals(0, txn.findNodes(PLATFORM).count());

            List<MutableGraphNode> foundByLabelMutable = txn.findNodesMutable(FERRY).toList();
            assertEquals(1, foundByLabelMutable.size());
            assertEquals(id, foundByLabelMutable.getFirst().getId());

            Station station = Victoria.fake();
            GraphNode findByItem = txn.findNode(station);
            assertNull(findByItem);

        }
    }

    @Test
    void shouldGetRelationships() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {

            MutableGraphNode nodeA = txn.createNode(ROUTE_STATION);
            MutableGraphNode nodeB = txn.createNode(STATION);

            Station station = Victoria.fake();
            Route route = TestEnv.getTramTestRoute();
            RouteStation routeStation = new RouteStation(station, route);
            nodeA.set(routeStation);

            GraphNode check = txn.findNode(routeStation);
            assertNotNull(check);

            List<GraphRelationship> initialSearch = txn.getRouteStationRelationships(routeStation, Outgoing);
            assertTrue(initialSearch.isEmpty());

            MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.DEPART);

            List<GraphRelationship> result = txn.getRouteStationRelationships(routeStation, Outgoing);
            assertFalse(result.isEmpty());

            assertTrue(result.contains(relationship));

            GraphRelationshipId id = relationship.getId();
            GraphRelationship viaTransaction = txn.getRelationshipById(id);
            assertNotNull(viaTransaction);
            assertEquals(id, viaTransaction.getId());
        }

    }

    @Test
    void shouldConvertToImmutable() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {

            MutableGraphNode nodeA = txn.createNode(BUS);
            GraphNodeId id = nodeA.getId();

            GraphTransaction immutable = txn.asImmutable();

            GraphNode find = immutable.getNodeById(id);
            assertEquals(id, find.getId());
        }
    }

    @Test
    void shouldFindNodesByDomainItem() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            Station station = Victoria.fake();

            MutableGraphNode nodeA = txn.createNode(station.getNodeLabel());
            nodeA.set(station);
            GraphNodeId id = nodeA.getId();
            MutableGraphNode nodeB = txn.createNode(station.getNodeLabel());
            nodeB.set(Bury.fake());

            GraphNode findByItem = txn.findNode(station);
            assertEquals(id, findByItem.getId());

            MutableGraphNode findMutable = txn.findNodeMutable(station);
            assertEquals(id, findMutable.getId());

            assertTrue(txn.hasAnyMatching(station.getNodeLabel(), station.getProp(), station.getId().getGraphId()));

            assertFalse(txn.hasAnyMatching(ROUTE_STATION, station.getProp(), station.getId().getGraphId()));
            assertFalse(txn.hasAnyMatching(station.getNodeLabel(), GraphPropertyKey.ROUTE_STATION_ID, station.getId().getGraphId()));
            assertFalse(txn.hasAnyMatching(station.getNodeLabel(), station.getProp(), "wrongId"));

            nodeA.delete(txn);

            assertFalse(txn.hasAnyMatching(station.getNodeLabel(), station.getProp(), station.getId().getGraphId()));

        }
    }

    @Test
    void shouldCreateRelationship() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode start = txn.createNode(FERRY);
            MutableGraphNode end = txn.createNode(TRAIN);

            MutableGraphRelationship relationship = start.createRelationshipTo(txn, end, FERRY_GOES_TO);

            assertTrue(relationship.isRelationship());
            assertFalse(relationship.isNode());

            assertEquals(FERRY_GOES_TO, relationship.getType());
            assertTrue(relationship.isType(FERRY_GOES_TO));
            assertFalse(relationship.isType(TRAIN_GOES_TO));

            GraphRelationshipId id = relationship.getId();
            assertEquals(new RelationshipIdInMemory(0), id);

            assertEquals(start, relationship.getStartNode(txn));
            assertEquals(end, relationship.getEndNode(txn));

            assertEquals(start.getId(), relationship.getStartNodeId(txn));
            assertEquals(end.getId(), relationship.getEndNodeId(txn));

        }
    }

    @Test
    void shouldQueryRelationship() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode start = txn.createNode(FERRY);
            MutableGraphNode end = txn.createNode(TRAIN);

            MutableGraphRelationship relationship = start.createRelationshipTo(txn, end, FERRY_GOES_TO);

            assertTrue(start.hasRelationship(txn, Outgoing, FERRY_GOES_TO));
            assertTrue(end.hasRelationship(txn, Incoming, FERRY_GOES_TO));

            GraphRelationship singleOutgoing = start.getSingleRelationship(txn, FERRY_GOES_TO, Outgoing);
            assertNotNull(singleOutgoing);
            assertEquals(relationship.getId(), singleOutgoing.getId());

            GraphRelationship singleIncoming = end.getSingleRelationship(txn, FERRY_GOES_TO, Incoming);
            assertNotNull(singleIncoming);
            assertEquals(relationship.getId(), singleIncoming.getId());
        }
    }

    @Test
    void shouldQueryRelationships() {
            try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
                MutableGraphNode start = txn.createNode(FERRY);
                MutableGraphNode end = txn.createNode(TRAIN);

                MutableGraphRelationship relationshipA = start.createRelationshipTo(txn, end, FERRY_GOES_TO);
                MutableGraphRelationship relationshipB = end.createRelationshipTo(txn, start, TRAIN_GOES_TO);

                assertTrue(start.hasRelationship(txn, Outgoing, FERRY_GOES_TO));
                assertTrue(end.hasRelationship(txn, Incoming, FERRY_GOES_TO));

                assertTrue(start.hasRelationship(txn, Incoming, TRAIN_GOES_TO));
                assertTrue(end.hasRelationship(txn, Outgoing, TRAIN_GOES_TO));

                GraphRelationship singleOutgoing = start.getSingleRelationship(txn, FERRY_GOES_TO, Outgoing);
                assertNotNull(singleOutgoing);
                assertEquals(relationshipA.getId(), singleOutgoing.getId());

                GraphRelationship singleIncoming = end.getSingleRelationship(txn, FERRY_GOES_TO, Incoming);
                assertNotNull(singleIncoming);
                assertEquals(relationshipA.getId(), singleIncoming.getId());

                List<GraphRelationship> atStart = start.getRelationships(txn, GraphDirection.Both, FERRY_GOES_TO, TRAIN_GOES_TO).toList();
                assertEquals(2, atStart.size());
                assertTrue(atStart.contains(relationshipA));
                assertTrue(atStart.contains(relationshipB));
            }
        }

        @Test
        void shouldQueryRelationshipsMutable() {
            try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
                MutableGraphNode start = txn.createNode(FERRY);
                MutableGraphNode end = txn.createNode(TRAIN);

                MutableGraphRelationship relationshipA = start.createRelationshipTo(txn, end, FERRY_GOES_TO);
                end.createRelationshipTo(txn, start, TRAIN_GOES_TO);

                assertTrue(start.hasRelationship(txn, Outgoing, FERRY_GOES_TO));
                assertTrue(end.hasRelationship(txn, Incoming, FERRY_GOES_TO));

                MutableGraphRelationship singleOutgoing = start.getSingleRelationshipMutable(txn, FERRY_GOES_TO, Outgoing);
                assertNotNull(singleOutgoing);
                assertEquals(relationshipA.getId(), singleOutgoing.getId());

                MutableGraphRelationship singleIncoming = end.getSingleRelationshipMutable(txn, FERRY_GOES_TO, Incoming);
                assertNotNull(singleIncoming);
                assertEquals(relationshipA.getId(), singleIncoming.getId());

                List<MutableGraphRelationship> atStart = start.getRelationshipsMutable(txn, Outgoing, FERRY_GOES_TO).toList();
                assertEquals(1, atStart.size());
                assertTrue(atStart.contains(relationshipA));
            }
        }

    @Test
    void shouldUpdateQueryStationsForRelationships() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode start = txn.createNode(FERRY);
            MutableGraphNode end = txn.createNode(TRAIN);

            MutableGraphRelationship relationship = start.createRelationshipTo(txn, end, FERRY_GOES_TO);
            end.createRelationshipTo(txn, start, TRAIN_GOES_TO);

            assertTrue(start.hasRelationship(txn, Outgoing, FERRY_GOES_TO));
            assertTrue(end.hasRelationship(txn, Incoming, FERRY_GOES_TO));

            Station stationA = Victoria.fake();
            Station stationB = Broadway.fake();

            start.set(stationA);
            end.set(stationB);

            IdFor<Station> beginId = relationship.getStartStationId();
            assertEquals(stationA.getId(), beginId);

            IdFor<Station> endId = relationship.getEndStationId();
            assertEquals(stationB.getId(), endId);
        }
    }

    @Test
    void shouldDeleteNodesAndRelationships() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode start = txn.createNode(FERRY);
            MutableGraphNode end = txn.createNode(TRAIN);

            MutableGraphRelationship relationshipA = start.createRelationshipTo(txn, end, FERRY_GOES_TO);
            MutableGraphRelationship relationshipB = end.createRelationshipTo(txn, start, TRAIN_GOES_TO);

            assertTrue(start.hasRelationship(txn, Outgoing, FERRY_GOES_TO));
            assertTrue(end.hasRelationship(txn, Incoming, FERRY_GOES_TO));
            assertTrue(start.hasRelationship(txn, Incoming, TRAIN_GOES_TO));
            assertTrue(end.hasRelationship(txn, Outgoing, TRAIN_GOES_TO));

            relationshipA.delete(txn);

            assertFalse(start.hasRelationship(txn, Outgoing, FERRY_GOES_TO));
            assertFalse(end.hasRelationship(txn, Incoming, FERRY_GOES_TO));
            assertTrue(start.hasRelationship(txn, Incoming, TRAIN_GOES_TO));
            assertTrue(end.hasRelationship(txn, Outgoing, TRAIN_GOES_TO));

            List<GraphRelationship> foundOutgoing = start.getRelationships(txn, Outgoing, FERRY_GOES_TO).toList();
            assertTrue(foundOutgoing.isEmpty());

            relationshipB.delete(txn);

            assertFalse(start.hasRelationship(txn, Outgoing, FERRY_GOES_TO));
            assertFalse(end.hasRelationship(txn, Incoming, FERRY_GOES_TO));
            assertFalse(start.hasRelationship(txn, Incoming, TRAIN_GOES_TO));
            assertFalse(end.hasRelationship(txn, Outgoing, TRAIN_GOES_TO));

            List<GraphRelationship> foundBoth = start.getRelationships(txn, Both, FERRY_GOES_TO, TRAIN_GOES_TO).toList();
            assertTrue(foundBoth.isEmpty());

            end.delete(txn);

            assertFalse(txn.hasAnyMatching(TRAIN));

            assertThrows(GraphException.class, () -> txn.getNodeById(end.getId()));

        }
    }

}
