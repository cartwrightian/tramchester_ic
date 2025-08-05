package com.tramchester.unit.graph.inMemory;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.Graph;
import com.tramchester.graph.core.inMemory.NodeIdInMemory;
import com.tramchester.graph.core.inMemory.RelationshipIdInMemory;
import com.tramchester.graph.core.inMemory.TransactionManager;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.tramchester.graph.reference.GraphLabel.FERRY;
import static com.tramchester.graph.reference.GraphLabel.TRAIN;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionManagerTest {
    private TransactionManager transactionManager;

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
        try (MutableGraphTransaction graphTransaction = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode node = graphTransaction.createNode(FERRY);

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
    void shouldCreateRelationship() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode start = txn.createNode(FERRY);
            MutableGraphNode end = txn.createNode(TRAIN);

            MutableGraphRelationship relationship = start.createRelationshipTo(txn, end, TransportRelationshipTypes.FERRY_GOES_TO);

            assertTrue(relationship.isRelationship());
            assertFalse(relationship.isNode());

            assertEquals(TransportRelationshipTypes.FERRY_GOES_TO, relationship.getType());

            GraphRelationshipId id = relationship.getId();
            assertEquals(new RelationshipIdInMemory(0), id);

            assertEquals(start, relationship.getStartNode(txn));
            assertEquals(end, relationship.getEndNode(txn));

            assertEquals(start.getId(), relationship.getStartNodeId(txn));
            assertEquals(end.getId(), relationship.getEndNodeId(txn));
        }
    }
}
