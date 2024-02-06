package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationGroupsGraphBuilder;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Direction;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_GROUPED;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
public class StationGroupsGraphBuilderTest {
    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;
    private static MutableGraphTransaction txn;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        testConfig = new IntegrationBusTestConfig(TestGroupType.performance);

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        componentContainer.get(StationGroupsGraphBuilder.Ready.class);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        txn = graphDatabase.beginTxMutable(Duration.ofMinutes(5));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        txn.close();
        componentContainer.close();
    }

    @Test
    void shouldHaveCostsWithinExpectedBounds() {
        Stream<ImmutableGraphNode> groupNodes = txn.findNodes(GraphLabel.GROUPED);
        final Duration walkingDuration = testConfig.getWalkingDuration();


        Set<ImmutableGraphNode> toOthers = groupNodes.filter(node -> node.hasRelationship(Direction.OUTGOING, GROUPED_TO_GROUPED)).collect(Collectors.toSet());

        toOthers.forEach(node -> {
            List<ImmutableGraphRelationship> outbounds = node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_GROUPED).toList();
            assertFalse(outbounds.isEmpty(), node.getAllProperties().toString());

            ImmutableGraphRelationship link = outbounds.get(0);
            Duration cost = link.getCost();
            assertTrue(cost.compareTo(walkingDuration) <=0, "got " + cost + " more than " + walkingDuration);

        });

    }

}
