package com.tramchester.unit.graph.neo4J;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.core.MutableGraphRelationship;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.core.neo4j.GraphTestHelperNeo4J;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.GraphPropsTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.tramchester.graph.GraphPropertyKey.TRIP_ID_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphPropsNeo4JTest {

    private static ComponentContainer componentContainer;
    private static UnitTestOfGraphConfig config;
    private MutableGraphTransaction txn;

    // See TransportDataFromFilesTramTest for test that gets this number
    private static final int maxTripsForService = 1535;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new UnitTestOfGraphConfig(GraphDBType.Neo4J);
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        txn = graphDatabase.beginTxMutable();
    }

    @AfterEach
    void afterEachTestRuns() {
        // no commit
        txn.close();
    }

    @Test
    void shouldPopulateTripIdsForServiceRelationshipsAsExpected() {

        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship serviceRelationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.TO_SERVICE);

        // add maxTripsForService trip id's to the relationship
        IdSet<Trip> unsortedTripIds = GraphPropsTest.addRandomTripIdsToRelationship(serviceRelationship);

        IdSet<Trip> fromService = serviceRelationship.getTripIds();
        assertEquals(maxTripsForService, fromService.size());
        assertTrue(unsortedTripIds.containsAll(fromService));

        if (config.getInMemoryGraph()) {
            // TODO do we care about ordering for in memory?
        } else {

            GraphTestHelperNeo4J graphTestHelper = new GraphTestHelperNeo4J();

            //MutableGraphTransaction neo4 = (MutableGraphTransactionNeo4J) txn;
            final Relationship relationship = graphTestHelper.getUnderlyingUnsafe(txn, serviceRelationship);

            List<IdFor<Trip>> sortedTripIds = unsortedTripIds.stream().
                    sorted(Comparator.comparing(IdFor::getGraphId)).
                    toList();

            String[] directFromRelationship = (String[]) relationship.getProperty(TRIP_ID_LIST.getText());
            assertEquals(maxTripsForService, directFromRelationship.length);

            // check sorted as expected
            for (int i = 0; i < directFromRelationship.length; i++) {
                final String tripIdText = sortedTripIds.get(i).getGraphId();
                assertEquals(tripIdText, directFromRelationship[i], "mismatch on " + i);
            }

            unsortedTripIds.forEach(tripId -> assertTrue(nodeA.hasOutgoingServiceMatching(txn, tripId), "Failed for " + tripId));
        }
    }

}
