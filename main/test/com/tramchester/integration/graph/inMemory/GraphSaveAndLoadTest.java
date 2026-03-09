package com.tramchester.integration.graph.inMemory;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.core.inMemory.persist.GraphPersistence;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.core.GraphDirection.Incoming;
import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.TransportRelationshipTypes.TO_SERVICE;
import static com.tramchester.graph.reference.TransportRelationshipTypes.TRAM_GOES_TO;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("OOM issues with gradle")
public class GraphSaveAndLoadTest {
    private static final Path GRAPH_PATH = Path.of("testData/GraphSaveAndLoadTest/graph");
    private static GuiceContainerDependencies componentContainer;
    private static TramchesterConfig config;
    private GraphPersistence graphPersistence;
    private static Path reltationshipsFilename;
    private static Path nodesFilename;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new LocalConfig(GraphDBType.InMemory);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();

        reltationshipsFilename = GRAPH_PATH.resolve(GraphPersistence.RELATIONSHIPS_FILENAME);
        nodesFilename = GRAPH_PATH.resolve(GraphPersistence.NODES_FILENAME);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        Files.deleteIfExists(reltationshipsFilename);
        Files.deleteIfExists(nodesFilename);
        Files.deleteIfExists(GRAPH_PATH);
    }

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        graphPersistence = componentContainer.get(GraphPersistence.class);


        Files.deleteIfExists(reltationshipsFilename);
        Files.deleteIfExists(nodesFilename);
    }

    @AfterEach
    void onceAfterEachTestRuns() throws IOException {
        Files.deleteIfExists(reltationshipsFilename);
        Files.deleteIfExists(nodesFilename);
        Files.deleteIfExists(GRAPH_PATH);
    }

    @Test
    void shouldSanityCheckConfig() {
        Path testPath = config.getGraphDBConfig().getDbPath();
        assertEquals(GRAPH_PATH, testPath);
    }

    @Test
    void shouldSerialiseToFileWithoutError() {
        GraphInMemoryServiceManager serviceManager = componentContainer.get(GraphInMemoryServiceManager.class);

        boolean result = graphPersistence.save(GRAPH_PATH, serviceManager);
        assertTrue(result);
        assertTrue(Files.exists(reltationshipsFilename));
        assertTrue(Files.exists(nodesFilename));

        GraphCore graph = serviceManager.getGraphCore();
        NodesAndEdges expected = graph.getNodesAndEdges();

//        NodesAndEdges result = GraphPersistence.load(GRAPH_PATH);

        GraphIdFactory idFactory = new GraphIdFactory();
        GraphCore loaded = graphPersistence.loadDBFrom(GRAPH_PATH, idFactory);

        assertEquals(expected, loaded.getNodesAndEdges());
    }

    @Test
    void shouldSanityCheckComparison() {
        GraphInMemoryServiceManager serviceManager = componentContainer.get(GraphInMemoryServiceManager.class);
        GraphCore core = serviceManager.getGraphCore();

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(core, core, txn);
            assertTrue(GraphCore.same(core, core));
        }
    }

    @Test
    void shouldSaveAndLoadAndCompare() {
        GraphInMemoryServiceManager serviceManager = componentContainer.get(GraphInMemoryServiceManager.class);
        GraphCore expected = serviceManager.getGraphCore();

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        GraphIdFactory idFactory = componentContainer.get(GraphIdFactory.class);

        boolean savedOk = graphPersistence.save(GRAPH_PATH, serviceManager);
        assertTrue(savedOk);
        assertTrue(Files.exists(reltationshipsFilename));
        assertTrue(Files.exists(nodesFilename));

        GraphCore result = graphPersistence.loadDBFrom(GRAPH_PATH, idFactory);

        assertEquals(expected.getNodesAndEdges(), result.getNodesAndEdges());

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            assertNotEquals(0, txn.numberOf(TRAM_GOES_TO), "sanity check on counters failed");

            checkSame(expected, result, txn);
        }

        //expected.stop();
        result.stop();
    }

    @Test
    void shouldHaveTestForActiveFiltering() {
        fail("TODO add test");
    }

    @Test
    void shouldNotHaveMissingTripsOnAnyServiceRelations() {

        boolean savedOk = graphPersistence.save(GRAPH_PATH, componentContainer.get(GraphInMemoryServiceManager.class));
        assertTrue(savedOk);

        @NotNull GraphInMemoryServiceManager serviceManager = CreateGraphDatabaseInMemory(GRAPH_PATH, componentContainer);
        DataSourceRepository dataSourceRepository = componentContainer.get(DataSourceRepository.class);
        GraphDatabaseInMemory graphDatabase = new GraphDatabaseInMemory(serviceManager, config, dataSourceRepository,
                new GraphFilterActive(false));

        graphDatabase.start();

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            Stream<GraphNode> nodes = txn.findNodes(GraphLabel.ROUTE_STATION);

            List<GraphNode> haveMissingTrips = nodes.
                    filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            anyMatch(rel -> rel.getTripIds().isEmpty())).toList();

            List<GraphNode> allMissingTripIds = haveMissingTrips.stream().
                    filter(node -> node.getRelationships(txn, Outgoing, TO_SERVICE).
                            allMatch(rel -> rel.getTripIds().isEmpty())).toList();

            assertEquals(0, allMissingTripIds.size());
            assertEquals(0, haveMissingTrips.size());
        }
    }

    @Test
    void shouldLoadConsistently() {
        boolean savedOk = graphPersistence.save(GRAPH_PATH, componentContainer.get(GraphInMemoryServiceManager.class));
        assertTrue(savedOk);

        GraphInMemoryServiceManager serviceManager = componentContainer.get(GraphInMemoryServiceManager.class);
        GraphCore graphA = serviceManager.getGraphCore();

        GraphIdFactory idFactory = componentContainer.get(GraphIdFactory.class);
        GraphCore graphB = graphPersistence.loadDBFrom(GRAPH_PATH, idFactory);

        GraphCore.same(graphA, graphB);

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            checkSame(graphA, graphB, txn);
        }
    }

    private static void checkSame(GraphCore expected, GraphCore result, GraphTransaction txnForExpected) {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            assertEquals(expected.getNumberOf(relationshipType), result.getNumberOf(relationshipType), "wrong for " + relationshipType);
        }

        for(GraphLabel label : GraphLabel.values()) {
            final Set<GraphNodeInMemory> expectedNodes = expected.findNodesMutable(label).collect(Collectors.toSet());
            final Set<GraphNodeInMemory> resultNodes = result.findNodesMutable(label).collect(Collectors.toSet());
            SetUtils.SetView<GraphNodeInMemory> comparison = SetUtils.disjunction(expectedNodes, resultNodes);
            assertTrue(comparison.isEmpty(), "mismatch finding nodes by label for " + label);

            for (GraphNodeInMemory expectedNode : expectedNodes) {
                final GraphNodeInMemory resultNode = result.getNodeMutable(expectedNode.getId());
                assertEquals(expectedNode.getId(), resultNode.getId());
                assertEquals(expectedNode.getProperties(), resultNode.getProperties());

                checkRelationships(txnForExpected, Outgoing, expectedNode, result);
                checkRelationships(txnForExpected, Incoming, expectedNode, result);
            }
        }

        assertTrue(GraphCore.same(expected, result), "graph core same failed");
    }

    private static void checkRelationships(GraphTransaction txn, GraphDirection direction,
                                           GraphNodeInMemory expected, GraphCore result) {

        final NodeIdInMemory nodeId = expected.getId();

        long numForExpected = expected.getRelationships(txn, direction, EnumSet.allOf(TransportRelationshipTypes.class)).count();
        long resultCount = result.findRelationshipsImmutableFor(nodeId, direction).count();
        assertEquals(numForExpected, resultCount, "for " + expected.getAllProperties());

        for(TransportRelationshipTypes transportRelationshipType : TransportRelationshipTypes.values()) {

            final Set<GraphRelationship> expectedRelationships = expected.
                    getRelationships(txn, direction, transportRelationshipType).
                    collect(Collectors.toSet());

            final Set<GraphRelationship> resultRelationships = result.
                    findRelationshipsImmutableFor(nodeId, direction).
                    filter(relat -> relat.isType(transportRelationshipType)).
                    collect(Collectors.toSet());

            Set<GraphRelationship> diff = SetUtils.disjunction(expectedRelationships, resultRelationships);

            assertTrue(diff.isEmpty(), "diff:" + diff +  " failed for " + transportRelationshipType + " expected:" +
                    expectedRelationships + " vs result:" + resultRelationships);
        }
    }

    public @NotNull GraphInMemoryServiceManager CreateGraphDatabaseInMemory(Path path, GuiceContainerDependencies container) {
        ProvidesNow providesNow = container.get(ProvidesNow.class);
        GraphIdFactory idFactory = container.get(GraphIdFactory.class);
        GraphDatabaseStoredVersions storedVersion = container.get(GraphDatabaseStoredVersions.class);
        //serviceManager.loadFrom(path);
        return new GraphInMemoryServiceManager(idFactory, storedVersion, providesNow, config, graphPersistence);
    }

    private static class LocalConfig extends IntegrationTramTestConfig {
        public LocalConfig(GraphDBType graphDBType) {
            super(graphDBType);
        }

        @Override
        public Path getCacheFolder() {
            return Path.of("testData/cache");
        }

        @Override
        public GraphDBConfig getGraphDBConfig() {
            return new GraphDBConfig() {
                @Override
                public Path getDbPath() {
                    return GRAPH_PATH;
                }

                @Override
                public Boolean enableDiagnostics() {
                    return false;
                }
            };
        }
    }
}
