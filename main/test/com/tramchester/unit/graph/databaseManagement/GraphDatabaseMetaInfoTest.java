package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.DataSourceRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.DataSourceID.naptanxml;
import static com.tramchester.domain.DataSourceID.tfgm;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

public class GraphDatabaseMetaInfoTest extends EasyMockSupport {

    private GraphDatabaseMetaInfo databaseMetaInfo;
    private MutableGraphTransaction transaction;
    private Node node;

    @BeforeEach
    public void beforeAnyTestRuns() {
        node = createMock(Node.class);
        transaction = createMock(MutableGraphTransaction.class);
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
    void shouldCreateVersionsNode() {

        MutableGraphNode graphNode = createMock(MutableGraphNode.class);
        EasyMock.expect(graphNode.getNode()).andStubReturn(node);

        DataSourceInfo infoA = new DataSourceInfo(tfgm, "4.3", LocalDateTime.MAX, EnumSet.of(Tram));
        DataSourceInfo infoB = new DataSourceInfo(naptanxml, "9.6", LocalDateTime.MIN, EnumSet.of(Bus));

        Set<DataSourceInfo> sourceInfo = new HashSet<>(Arrays.asList(infoA, infoB));

        DataSourceRepository dataSourceRepos = createMock(DataSourceRepository.class);
        EasyMock.expect(dataSourceRepos.getDataSourceInfo()).andReturn(sourceInfo);

        EasyMock.expect(transaction.createNode(GraphLabel.VERSION)).andReturn(graphNode);
//        node.setProperty("tfgm", "4.3");
//        EasyMock.expectLastCall();
//        node.setProperty("naptanxml", "9.6");
//        EasyMock.expectLastCall();
        graphNode.set(infoA);
        graphNode.set(infoB);

        replayAll();
        databaseMetaInfo.createVersionNode(transaction, dataSourceRepos);
        verifyAll();
    }

}
