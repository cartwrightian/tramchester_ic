package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
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
    private GraphTransaction transaction;
    private Node node;

    @BeforeEach
    public void beforeAnyTestRuns() {
        node = createMock(Node.class);
        transaction = createMock(GraphTransaction.class);
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

        GraphNode graphNode = createMock(GraphNode.class);

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

        GraphNode graphNode = createMock(GraphNode.class);
        EasyMock.expect(transaction.createNode(GraphLabel.NEIGHBOURS_ENABLED)).andReturn(graphNode);

        replayAll();
        databaseMetaInfo.setNeighboursEnabled(transaction);
        verifyAll();
    }

    @Test
    void shouldCreateVersionsNode() {

        GraphNode graphNode = createMock(GraphNode.class);
        EasyMock.expect(graphNode.getNode()).andStubReturn(node);

        Set<DataSourceInfo> sourceInfo = new HashSet<>();
        sourceInfo.add(new DataSourceInfo(tfgm, "4.3", LocalDateTime.MAX, EnumSet.of(Tram)));
        sourceInfo.add(new DataSourceInfo(naptanxml, "9.6", LocalDateTime.MIN, EnumSet.of(Bus)));

        DataSourceRepository dataSourceRepos = createMock(DataSourceRepository.class);
        EasyMock.expect(dataSourceRepos.getDataSourceInfo()).andReturn(sourceInfo);

        EasyMock.expect(transaction.createNode(GraphLabel.VERSION)).andReturn(graphNode);
        node.setProperty("tfgm", "4.3");
        EasyMock.expectLastCall();
        node.setProperty("naptanxml", "9.6");
        EasyMock.expectLastCall();

        replayAll();
        databaseMetaInfo.createVersionNode(transaction, dataSourceRepos);
        verifyAll();
    }

}
