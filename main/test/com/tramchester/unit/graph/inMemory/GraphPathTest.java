package com.tramchester.unit.graph.inMemory;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.core.inMemory.Graph;
import com.tramchester.graph.core.inMemory.NodeIdInMemory;
import com.tramchester.graph.core.inMemory.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.tramchester.graph.reference.GraphLabel.FERRY;
import static com.tramchester.graph.reference.GraphLabel.TRAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphPathTest {
    private TransactionManager transactionManager;

    // TODO Check throws after delete including labels

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesNow = new ProvidesLocalNow();
        Graph graph = new Graph();
        transactionManager = new TransactionManager(providesNow, graph);
    }

    @Disabled("WIP")
    @Test
    void shouldCreateNode() {
        try (MutableGraphTransaction txn = transactionManager.createTransaction(Duration.ofMinutes(1))) {
            MutableGraphNode node = txn.createNode(FERRY);

            assertTrue(node.isNode());
            assertFalse(node.isRelationship());

            fail("todo");
        }
    }
}
