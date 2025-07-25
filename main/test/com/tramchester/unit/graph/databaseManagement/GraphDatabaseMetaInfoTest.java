package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.dataimport.URLStatus;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;

import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.DataSourceID.*;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

public class GraphDatabaseMetaInfoTest extends EasyMockSupport {

    private GraphDatabaseMetaInfo databaseMetaInfo;
    private MutableGraphTransactionNeo4J transaction;
    private Node node;

    @BeforeEach
    public void beforeAnyTestRuns() {
        node = createMock(Node.class);
        transaction = createMock(MutableGraphTransactionNeo4J.class);
        databaseMetaInfo = new GraphDatabaseMetaInfo();
    }

    @Test
    void shouldCheckForNeighboursNodePresent() {

        EasyMock.expect(transaction.hasAnyMatching(GraphLabel.NEIGHBOURS_ENABLED)).andReturn(true);

        replayAll();
        boolean result = databaseMetaInfo.isNeighboursEnabled(transaction);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckForNeighboursNodeMissing() {

        EasyMock.expect(transaction.hasAnyMatching(GraphLabel.NEIGHBOURS_ENABLED)).andReturn(false);

        replayAll();
        boolean result = databaseMetaInfo.isNeighboursEnabled(transaction);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCheckForVersionNodeMissing() {

        EasyMock.expect(transaction.hasAnyMatching(GraphLabel.VERSION)).andReturn(false);

        replayAll();
        boolean result = databaseMetaInfo.hasVersionInfo(transaction);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCheckForVersionNodePresent() {

        EasyMock.expect(transaction.hasAnyMatching(GraphLabel.VERSION)).andReturn(true);

        replayAll();
        boolean result = databaseMetaInfo.hasVersionInfo(transaction);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldGetVersionMapFromNode() {
        Map<String, Object> versionMap = new HashMap<>();
        versionMap.put("A", "4.2");
        versionMap.put("ZZZ", "81.91");

        ImmutableGraphNode graphNode = createMock(ImmutableGraphNode.class);

        EasyMock.expect(transaction.findNodes(GraphLabel.VERSION)).andReturn(Stream.of(graphNode));
        EasyMock.expect(graphNode.getAllProperties()).andReturn(versionMap);

        replayAll();
        Map<String, String> results = databaseMetaInfo.getVersions(transaction);
        verifyAll();

        assertEquals(2, results.size());
        assertTrue(results.containsKey("A"));
        assertEquals(results.get("A"), "4.2");
        assertTrue(results.containsKey("ZZZ"));
        assertEquals(results.get("ZZZ"), "81.91");
    }

    @Test
    void shouldSetNeighbourNode() {

        MutableGraphNode graphNode = createMock(MutableGraphNode.class);
        EasyMock.expect(transaction.createNode(GraphLabel.NEIGHBOURS_ENABLED)).andReturn(graphNode);

        replayAll();
        databaseMetaInfo.setNeighboursEnabled(transaction);
        verifyAll();
    }

    @Test
    void shouldGetBoundsMatch() {
        ImmutableGraphNode graphNode = createMock(ImmutableGraphNode.class);
        BoundingBox bounds = TestEnv.getGreaterManchester();

        EasyMock.expect(transaction.hasAnyMatching(GraphLabel.BOUNDS)).andReturn(true);
        EasyMock.expect(transaction.findNodes(GraphLabel.BOUNDS)).andReturn(Stream.of(graphNode));
        EasyMock.expect(graphNode.getBounds()).andReturn(bounds);

        replayAll();
        boolean result = databaseMetaInfo.boundsMatch(transaction, bounds);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldGetBoundsMisMatch() {
        ImmutableGraphNode graphNode = createMock(ImmutableGraphNode.class);
        BoundingBox boundsA = TestEnv.getGreaterManchester();
        BoundingBox boundsB = TestEnv.getNationalTrainBounds();

        EasyMock.expect(transaction.hasAnyMatching(GraphLabel.BOUNDS)).andReturn(true);
        EasyMock.expect(transaction.findNodes(GraphLabel.BOUNDS)).andReturn(Stream.of(graphNode));
        EasyMock.expect(graphNode.getBounds()).andReturn(boundsB);

        replayAll();
        boolean result = databaseMetaInfo.boundsMatch(transaction, boundsA);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCreateBoundsNode() {
        BoundingBox bounds = TestEnv.getGreaterManchester();

        MutableGraphNode graphNode = createMock(MutableGraphNode.class);
        EasyMock.expect(transaction.hasAnyMatching(GraphLabel.BOUNDS)).andReturn(false);
        EasyMock.expect(transaction.createNode(GraphLabel.BOUNDS)).andReturn(graphNode);
        graphNode.setBounds(bounds);
        EasyMock.expectLastCall();

        replayAll();
        databaseMetaInfo.setBounds(transaction, bounds);
        verifyAll();
    }

    @Test
    void shouldCreateVersionsNode() {

        MutableGraphNode graphNode = createMock(MutableGraphNode.class);
        EasyMock.expect(graphNode.getNode()).andStubReturn(node);

        DataSourceInfo infoA = new DataSourceInfo(tfgm, "4.3", URLStatus.invalidTime, EnumSet.of(Tram));
        DataSourceInfo infoB = new DataSourceInfo(naptanxml, "9.6", URLStatus.invalidTime, EnumSet.of(Bus));

        Set<DataSourceInfo> sourceInfo = new HashSet<>(Arrays.asList(infoA, infoB));

        DataSourceRepository dataSourceRepos = createMock(DataSourceRepository.class);
        EasyMock.expect(dataSourceRepos.getDataSourceInfo()).andReturn(sourceInfo);

        EasyMock.expect(transaction.createNode(GraphLabel.VERSION)).andReturn(graphNode);

        graphNode.set(infoA);
        graphNode.set(infoB);

        replayAll();
        databaseMetaInfo.createVersionNode(transaction, dataSourceRepos);
        verifyAll();
    }

}
