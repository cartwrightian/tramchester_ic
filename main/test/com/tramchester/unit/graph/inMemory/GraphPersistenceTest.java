package com.tramchester.unit.graph.inMemory;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.domain.Platform;
import com.tramchester.domain.collections.ImmutableEnumSetImpl;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.core.inMemory.persist.GraphPersistence;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.reference.TransportRelationshipTypes.BOARD;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static org.junit.jupiter.api.Assertions.*;

public class GraphPersistenceTest extends EasyMockSupport {
    private static final Path GRAPH_PATH = Path.of("testData/GraphSaveAndLoadTest/graph");
    private Path relationshipsFilename;
    private Path nodesFilename;
    private GraphPersistence graphPersistence;
    private NodesAndEdges nodesAndEdges;
    private Platform buryPlatform;
    private GraphNodeInMemory versionNode;
    private TramTime time;
    private TramDate date;


    @BeforeAll
    public static void onceBeforeAnyTestRuns() throws IOException {
        Files.deleteIfExists(GRAPH_PATH);
    }

    @AfterAll
    public static void onceAfterAllTestsRun() throws IOException {
        Files.deleteIfExists(GRAPH_PATH);
    }

    @BeforeEach
    public void onceBeforeEachTestRuns() throws IOException {
        relationshipsFilename = GRAPH_PATH.resolve(GraphPersistence.RELATIONSHIPS_FILENAME);
        nodesFilename = GRAPH_PATH.resolve(GraphPersistence.NODES_FILENAME);

        time = TramTime.of(13, 44);
        date = TramDate.of(2026, 12, 21);

        buryPlatform = TestEnv.createPlatformFor(Bury.fake(), "42");

        Files.deleteIfExists(relationshipsFilename);
        Files.deleteIfExists(nodesFilename);

        GetsFileModTime getFileModeTime = new GetsFileModTime();
        graphPersistence = new GraphPersistence(getFileModeTime);

        nodesAndEdges = new NodesAndEdges();

        GraphNodeInMemory nodeA = new GraphNodeInMemory(new NodeIdInMemory(1), ImmutableEnumSetImpl.of(GraphLabel.STATION), false);
        nodeA.setTransportMode(Tram);
        nodesAndEdges.addNode(nodeA.getId(), nodeA);

        GraphNodeInMemory nodeB = new GraphNodeInMemory(new NodeIdInMemory(2), ImmutableEnumSetImpl.of(GraphLabel.PLATFORM), false);
        nodeB.setPlatformNumber(buryPlatform);
        nodeB.set(buryPlatform.getStation());
        nodesAndEdges.addNode(nodeB.getId(), nodeB);

        versionNode = new GraphNodeInMemory(new NodeIdInMemory(3), ImmutableEnumSetImpl.of(GraphLabel.VERSION), false);
        versionNode.setTime(time);
        versionNode.setStartDate(date);
        nodesAndEdges.addNode(versionNode.getId(), versionNode);

        GraphRelationshipInMemory relationship = new GraphRelationshipInMemory(BOARD, new RelationshipIdInMemory(1), nodeA.getId(), nodeB.getId(), false);
        nodesAndEdges.putRelationship(relationship.getId(), relationship);

    }

    @AfterEach
    public void onceAfterEachTestRuns() throws IOException {
        Files.deleteIfExists(relationshipsFilename);
        Files.deleteIfExists(nodesFilename);
    }

    @Test
    void shouldSaveGraph() throws IOException {
        GraphInMemoryServiceManager serviceManager = createMock(GraphInMemoryServiceManager.class);
        GraphCore graphCode = createMock(GraphCore.class);

        EasyMock.expect(graphCode.findNodesImmutable(GraphLabel.VERSION)).andReturn(Stream.of(versionNode));

        EasyMock.expect(serviceManager.getGraphCore()).andReturn(graphCode);
        EasyMock.expect(graphCode.getNodesAndEdges()).andReturn(nodesAndEdges);

        replayAll();
        boolean saved = graphPersistence.save(GRAPH_PATH, serviceManager);
        verifyAll();

        assertTrue(saved);
        assertTrue(Files.exists(nodesFilename),"missing nodes");
        assertTrue(Files.exists(relationshipsFilename), "missing relationships");

        FileTime modTime = Files.getLastModifiedTime(GRAPH_PATH);

        ZonedDateTime expected = ZonedDateTime.of(date.toLocalDate(), time.asLocalTime(), TramchesterConfig.TimeZoneId);

        assertEquals(expected.toInstant(), modTime.toInstant());
    }

    @Test
    void shouldSkipSaveGraphIfAlreadyPresentWithCorrectTime() {
        GraphInMemoryServiceManager serviceManager = createMock(GraphInMemoryServiceManager.class);
        GraphCore graphCode = createMock(GraphCore.class);

        EasyMock.expect(graphCode.findNodesImmutable(GraphLabel.VERSION)).andReturn(Stream.of(versionNode));
        EasyMock.expect(graphCode.findNodesImmutable(GraphLabel.VERSION)).andReturn(Stream.of(versionNode));

        EasyMock.expect(serviceManager.getGraphCore()).andReturn(graphCode);
        EasyMock.expect(serviceManager.getGraphCore()).andReturn(graphCode);

        EasyMock.expect(graphCode.getNodesAndEdges()).andReturn(nodesAndEdges);

        replayAll();
        boolean firstSave = graphPersistence.save(GRAPH_PATH, serviceManager);
        boolean secondSave = graphPersistence.save(GRAPH_PATH, serviceManager);
        verifyAll();

        assertTrue(firstSave);
        assertFalse(secondSave);

    }

    @Test
    void shouldSaveAgainIfOutOfDate() throws IOException {
        GraphInMemoryServiceManager serviceManager = createMock(GraphInMemoryServiceManager.class);
        GraphCore graphCode = createMock(GraphCore.class);

        EasyMock.expect(graphCode.findNodesImmutable(GraphLabel.VERSION)).andReturn(Stream.of(versionNode));
        EasyMock.expect(graphCode.findNodesImmutable(GraphLabel.VERSION)).andReturn(Stream.of(versionNode));

        EasyMock.expect(serviceManager.getGraphCore()).andReturn(graphCode);
        EasyMock.expect(serviceManager.getGraphCore()).andReturn(graphCode);

        EasyMock.expect(graphCode.getNodesAndEdges()).andReturn(nodesAndEdges);
        EasyMock.expect(graphCode.getNodesAndEdges()).andReturn(nodesAndEdges);

        ZonedDateTime outOfDate = ZonedDateTime.of(date.toLocalDate().minusMonths(1), time.asLocalTime(), TramchesterConfig.TimeZoneId);

        replayAll();
        boolean firstSave = graphPersistence.save(GRAPH_PATH, serviceManager);
        Files.setLastModifiedTime(GRAPH_PATH, FileTime.from(outOfDate.toInstant()));
        boolean secondSave = graphPersistence.save(GRAPH_PATH, serviceManager);
        verifyAll();

        assertTrue(firstSave);
        assertTrue(secondSave);

    }

    @Test
    void shouldSaveAndLoadGraph() {
        GraphInMemoryServiceManager serviceManager = createMock(GraphInMemoryServiceManager.class);
        GraphCore graphCode = createMock(GraphCore.class);

        EasyMock.expect(graphCode.findNodesImmutable(GraphLabel.VERSION)).andReturn(Stream.of(versionNode));
        EasyMock.expect(serviceManager.getGraphCore()).andReturn(graphCode);
        EasyMock.expect(graphCode.getNodesAndEdges()).andReturn(nodesAndEdges);

        GraphIdFactory idFactory = new GraphIdFactory();

        replayAll();
        graphPersistence.save(GRAPH_PATH, serviceManager);
        GraphCore result = graphPersistence.loadDBFrom(GRAPH_PATH, idFactory);
        verifyAll();

        List<GraphNodeInMemory> stations = result.findNodesMutable(GraphLabel.STATION).toList();
        assertEquals(1, stations.size());
        GraphNodeInMemory station = stations.getFirst();
        assertEquals(Tram, station.getTransportMode());

        List<GraphNodeInMemory> platforms = result.findNodesMutable(GraphLabel.PLATFORM).toList();
        assertEquals(1, platforms.size());
        GraphNodeInMemory platform = platforms.getFirst();
        assertEquals(buryPlatform.getId(), platform.getPlatformId());

        assertEquals(1, result.getNumberOf(BOARD));
    }
}
