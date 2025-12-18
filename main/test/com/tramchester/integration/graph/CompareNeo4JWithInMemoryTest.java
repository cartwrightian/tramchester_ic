package com.tramchester.integration.graph;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeProperties;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.neo4j.NumberOfNodesAndRelationshipsRepositoryNeo4J;
import com.tramchester.integration.graph.inMemory.RouteCalculatorInMemoryTest;
import com.tramchester.integration.testSupport.GraphComparisons;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.GraphDBType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;

import static com.tramchester.graph.core.GraphDirection.Incoming;
import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.TransportRelationshipTypes.ENTER_PLATFORM;
import static com.tramchester.integration.graph.inMemory.GraphSaveAndLoadTest.CreateGraphDatabaseInMemory;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled("WIP")
public class CompareNeo4JWithInMemoryTest {

    private static GuiceContainerDependencies componentContainerNeo4J;

    private GuiceContainerDependencies componentContainerInMemory;
    private static TramchesterConfig config;

    private NumberOfNodesAndRelationshipsRepositoryInMemory inMemoryCounts;
    private NumberOfNodesAndRelationshipsRepositoryNeo4J neo4JCounts;
    private StationRepository stationRepository;
    private GraphTransaction txnInMem;
    private GraphTransaction txnNeo4J;
    private Duration maxJourneyDuration;
    private int maxNumResults;

    private RouteCalculatorTestFacade calculatorInMem;
    private RouteCalculatorTestFacade calculatorNeo4J;


    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig(GraphDBType.InMemory);
        TramchesterConfig configNeo4J = new IntegrationTramTestConfig(GraphDBType.Neo4J);

        componentContainerNeo4J = new ComponentsBuilder().create(configNeo4J, TestEnv.NoopRegisterMetrics());
        componentContainerNeo4J.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        componentContainerInMemory = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainerInMemory.initialise();

        inMemoryCounts = componentContainerInMemory.get(NumberOfNodesAndRelationshipsRepositoryInMemory.class);
        neo4JCounts = componentContainerNeo4J.get(NumberOfNodesAndRelationshipsRepositoryNeo4J.class);

        stationRepository = componentContainerInMemory.get(StationRepository.class);

        GraphDatabaseInMemory dbInMemory = componentContainerInMemory.get(GraphDatabaseInMemory.class);
        txnInMem = dbInMemory.beginTx();

        GraphDatabaseNeo4J dbNeo4J = componentContainerNeo4J.get(GraphDatabaseNeo4J.class);
        txnNeo4J = dbNeo4J.beginTx();

        calculatorInMem = new RouteCalculatorTestFacade(componentContainerInMemory, txnInMem);
        calculatorNeo4J = new RouteCalculatorTestFacade(componentContainerNeo4J, txnNeo4J);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        maxNumResults = config.getMaxNumResults();

    }

    @AfterEach
    void onceAfterEachTest() {
        txnInMem.close();
        txnNeo4J.close();
        componentContainerInMemory.close();
    }

    @AfterAll
    static void onceAfterAll() {
        componentContainerNeo4J.close();
    }

    @Test
    void shouldHaveSameCountsForNodes() {
        for(GraphLabel label : GraphLabel.values()) {
            long inMem = inMemoryCounts.numberOf(label);
            long neo4J = neo4JCounts.numberOf(label);
            assertEquals(neo4J, inMem, "Mismatch for " + label);
        }
    }

    @Test
    void compareWithGraphWithFailure() {
        Graph result = SaveGraph.loadDBFrom(RouteCalculatorInMemoryTest.GRAPH_FILENAME_FAIL);

        GraphDatabaseInMemory inMemoryDB = CreateGraphDatabaseInMemory(result, componentContainerInMemory);
        inMemoryDB.start();

        Station station = VeloPark.from(stationRepository);

        Optional<GraphNodeInMemory> findInLoadedDB = result.findNodes(GraphLabel.STATION).
                filter(GraphNodeProperties::hasStationId).
                filter(node -> node.getStationId().equals(station.getId())).
                findFirst();

        assertFalse(findInLoadedDB.isEmpty());

        GraphNodeInMemory inMemoryNode = findInLoadedDB.get();

        final GraphNode neo4JNode = txnNeo4J.findNode(station);

        try (GraphTransaction inMemoryTxn = inMemoryDB.beginTx()) {
            GraphComparisons graphComparisons = new GraphComparisons(txnNeo4J, inMemoryTxn);
            graphComparisons.visitMatchedNodes(neo4JNode, inMemoryNode, 5);
        }

    }

    @Test
    void shouldHaveSameCountsForRelationships() {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            long inMem = inMemoryCounts.numberOf(relationshipType);
            long neo4J = neo4JCounts.numberOf(relationshipType);
            assertEquals(neo4J, inMem, "Mismatch for " + relationshipType);
        }
    }

    @Test
    void shouldReproSpecificMismatchOnPlatformEntry() {
        IdFor<Station> stationId = Station.createId("9400ZZMASLE");
        Station station = stationRepository.getStationById(stationId);

        GraphNode nodeInMem = txnInMem.findNode(station);
        GraphNode nodeNeo4J = txnNeo4J.findNode(station);

        List<GraphRelationship> relationshipsNeo4J = nodeNeo4J.getRelationships(txnNeo4J, Outgoing, ENTER_PLATFORM).toList();
        List<GraphRelationship> relationshipsInMem = nodeInMem.getRelationships(txnInMem, Outgoing, ENTER_PLATFORM).toList();

        assertEquals(relationshipsNeo4J.size(), relationshipsInMem.size());

        GraphComparisons graphComparisons = new GraphComparisons(txnNeo4J, txnInMem);

        graphComparisons.checkRelationshipsMatch(relationshipsNeo4J, relationshipsInMem);

    }

    @Test
    void shouldHaveRelationshipsAtStationNodes() {
        checkForType(stationRepository.getStations());
    }

    @Test
    void shouldHaveRelationshipsAtPlatformNodes() {
        PlatformRepository platformRepository = componentContainerInMemory.get(PlatformRepository.class);
        Set<Platform> platforms = platformRepository.getPlatforms(EnumSet.of(TransportMode.Tram));
        checkForType(platforms);
    }

//    @Disabled("slow...")
//    @Test
//    void shouldHaveRelationshipsAtRouteStationNodes() {
//        checkForType(stationRepository.getRouteStations());
//    }

    @RepeatedTest(5)
    void shouldCheckConsistencyAtSpecificRouteStation() {
        Station station = HoltTown.from(stationRepository);

        Set<Route> pickUps = station.getPickupRoutes();
        Set<Route> dropOffs = station.getPickupRoutes();

        Set<Route> all = new HashSet<>(pickUps);
        all.addAll(dropOffs);

        all.forEach(route -> {
            final RouteStation routeStation = stationRepository.getRouteStation(station, route);
            checkConsistencyOf(routeStation);
        });

    }

    @Disabled("WIP")
    @Test
    void shouldCompareSameJourney() {

        TramDate when = TestEnv.testDay();
        TramTime time = TramTime.of(17,45);
        EnumSet<TransportMode> requestedModes = EnumSet.of(TransportMode.Tram);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 0,
                maxJourneyDuration, maxNumResults, requestedModes);

        TramStations begin = Altrincham;
        TramStations dest = OldTrafford;

        List<Journey> journeysInMem = sortedByArrivalTime(calculatorInMem.calculateRouteAsList(begin, dest, journeyRequest));
        assertFalse(journeysInMem.isEmpty(), journeyRequest.toString());

        List<Journey> journeysNeo4J = sortedByArrivalTime(calculatorNeo4J.calculateRouteAsList(begin, dest, journeyRequest));
        assertFalse(journeysNeo4J.isEmpty(), journeyRequest.toString());

        assertEquals(journeysNeo4J, journeysInMem);
    }

    @Disabled("WIP")
    @Test
    void shouldCheckForConsistencyWhenInMemFails() {
        TramDate when = TestEnv.testDay();
        TramTime time = TramTime.of(17,45);
        EnumSet<TransportMode> requestedModes = EnumSet.of(TransportMode.Tram);
        Duration maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        long maxNumResults = 3;

        final JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1, maxJourneyDuration,
                maxNumResults, requestedModes);
        List<Journey> journeys = calculatorInMem.calculateRouteAsList(Altrincham, Ashton, journeyRequest);
        assertFalse(journeys.isEmpty());
    }

//    @ParameterizedTest
//    @EnumSource(TramStations.class)
//    @Disabled("WIP - slow")
//    void shouldWalkGraphs(final TramStations tramStation) {
//        final Station item = tramStation.from(stationRepository);
//
//        // local due to timeouts
//        try (GraphTransaction localTransaction = dbNeo4J.beginTx()) {
//
//            final GraphNode inMemoryNode = txnInMem.findNode(item);
//            final GraphNode neo4JNode = localTransaction.findNode(item);
//
//            final GraphComparisons graphComparisons = new GraphComparisons(localTransaction, txnInMem);
//            graphComparisons.visitMatchedNodes(neo4JNode, inMemoryNode, 5);
//        }
//    }

    @Test
    void shouldWalkGraphForVeloPark() {
        Station item = VeloPark.from(stationRepository);

        final GraphNode inMemoryNode = txnInMem.findNode(item);
        final GraphNode neo4JNode = txnNeo4J.findNode(item);

        GraphComparisons graphComparisons = new GraphComparisons(txnNeo4J, txnInMem);
        graphComparisons.visitMatchedNodes(neo4JNode, inMemoryNode, 5);
    }


    private List<Journey> sortedByArrivalTime(final List<Journey> journeys) {
        List<Journey> result = new ArrayList<>(journeys);
        result.sort(Comparator.comparing(Journey::getArrivalTime));
        return result;
    }

    private <T extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> void checkForType(final Collection<T> items) {
        for(final T item : items) {
            checkConsistencyOf(item);
        }
    }

    private <T extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> void checkConsistencyOf(T item) {
        final GraphNode inMemoryNode = txnInMem.findNode(item);
        final GraphNode neo4JNode = txnNeo4J.findNode(item);
        assertEquals(neo4JNode.getLabels(), inMemoryNode.getLabels());

        GraphComparisons graphComparisons = new GraphComparisons(txnNeo4J, txnInMem);

        graphComparisons.checkProps(neo4JNode, inMemoryNode);
        graphComparisons.checkSameDirections(neo4JNode, inMemoryNode, Outgoing);
        graphComparisons.checkSameDirections(neo4JNode, inMemoryNode, Incoming);

    }

}
