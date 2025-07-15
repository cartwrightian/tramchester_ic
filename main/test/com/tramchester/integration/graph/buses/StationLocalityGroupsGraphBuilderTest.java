package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationGroupsGraphBuilder;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_GROUPED;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_PARENT;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
public class StationLocalityGroupsGraphBuilderTest {
    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;
    private static MutableGraphTransaction txn;
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig(TestGroupType.performance);

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        componentContainer.get(StationGroupsGraphBuilder.Ready.class);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        txn = graphDatabase.beginTxMutable(Duration.ofMinutes(5));
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        txn.close();
        componentContainer.close();
    }

    @Test
    void shouldHaveCostsWithinExpectedBounds() {
        Stream<ImmutableGraphNode> groupNodes = txn.findNodes(GraphLabel.GROUPED);
        final Duration walkingDuration = testConfig.getWalkingDuration();

        Set<ImmutableGraphNode> toOthers = groupNodes.filter(node -> node.hasRelationship(Direction.OUTGOING, GROUPED_TO_GROUPED)).collect(Collectors.toSet());

        assertFalse(toOthers.isEmpty());

        toOthers.forEach(node -> {
            List<ImmutableGraphRelationship> outbounds = node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_GROUPED).toList();
            assertFalse(outbounds.isEmpty(), node.getAllProperties().toString());

            ImmutableGraphRelationship link = outbounds.get(0);
            Duration cost = link.getCost();
            assertTrue(cost.compareTo(walkingDuration) <=0, "got " + cost + " more than " + walkingDuration);

        });

    }

    @Test
    void shouldHaveSpecificGroupWithExpectedRelationships() {
        KnownLocality bollington = KnownLocality.Bollington;
        IdFor<StationLocalityGroup> stationGroupId = bollington.getId();

        List<ImmutableGraphNode> nodes = txn.findNodes(GraphLabel.GROUPED).
                filter(node -> node.getAreaId().equals(bollington.getAreaId())).toList();

        assertEquals(1, nodes.size());

        ImmutableGraphNode stationGroupNode = nodes.get(0);

        assertEquals(stationGroupNode.getStationGroupId(), stationGroupId);

        List<ImmutableGraphRelationship> childLinks = stationGroupNode.getRelationships(txn,
                Direction.OUTGOING, TransportRelationshipTypes.GROUPED_TO_CHILD).toList();

        StationLocalityGroup group = stationGroupsRepository.getStationGroup(stationGroupId);

        LocationSet<Station> containedLocations = group.getAllContained();
        IdSet<Station> containedIds = containedLocations.stream().collect(IdSet.collector());
        assertFalse(containedIds.isEmpty());
        // same number of child nodes as locations in the group
        assertEquals(containedLocations.size(), childLinks.size());

        List<GraphNode> childNodes = childLinks.stream().map(relationship -> relationship.getEndNode(txn)).toList();
        IdSet<Station> childLocationIds = childNodes.stream().map(GraphNode::getStationId).collect(IdSet.idCollector());

        assertEquals(containedIds, childLocationIds);

        childNodes.forEach(childNode -> {
            assertTrue(childNode.hasRelationship(Direction.OUTGOING, GROUPED_TO_PARENT), childNode.getStationId().toString());
            ImmutableGraphRelationship toParent = childNode.getSingleRelationship(txn, GROUPED_TO_PARENT, Direction.OUTGOING);
            GraphNode endNode = toParent.getEndNode(txn);
            assertEquals(stationGroupNode.getId(), endNode.getId(), "wrong parent for " + childNode.getStationId());

            assertEquals(stationGroupId, toParent.getStationGroupId(), "missing for " + childNode.getStationId());
        });
    }

}
