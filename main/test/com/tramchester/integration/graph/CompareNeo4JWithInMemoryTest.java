package com.tramchester.integration.graph;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphDatabaseInMemory;
import com.tramchester.graph.core.inMemory.NumberOfNodesAndRelationshipsRepositoryInMemory;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.neo4j.NumberOfNodesAndRelationshipsRepositoryNeo4J;
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
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.TRANSPORT_MODES;
import static com.tramchester.graph.GraphPropertyKey.TRIP_ID_LIST;
import static com.tramchester.graph.reference.TransportRelationshipTypes.ENTER_PLATFORM;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("WIP")
public class CompareNeo4JWithInMemoryTest {

    private static GuiceContainerDependencies componentContainerInMemory;
    private static GuiceContainerDependencies componentContainerNeo4J;
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

        componentContainerInMemory = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainerInMemory.initialise();

        componentContainerNeo4J = new ComponentsBuilder().create(configNeo4J, TestEnv.NoopRegisterMetrics());
        componentContainerNeo4J.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
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
    }

    @AfterAll
    static void onceAfterAll() {
        componentContainerInMemory.close();
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
    void shouldHaveSameCountsForRelationships() {
        for(TransportRelationshipTypes relationshipType : TransportRelationshipTypes.values()) {
            long inMem = inMemoryCounts.numberOf(relationshipType);
            long neo4J = neo4JCounts.numberOf(relationshipType);
            assertEquals(neo4J, inMem, "Mismatch for " + relationshipType);
        }
    }

    @Test
    void shouldReproSpecificMismatchOnPlatformEnrty() {
        IdFor<Station> stationId = Station.createId("9400ZZMASLE");
        Station station = stationRepository.getStationById(stationId);

        GraphNode nodeInMem = txnInMem.findNode(station);
        GraphNode nodeNeo4J = txnNeo4J.findNode(station);

        List<GraphRelationship> relationshipsNeo4J = nodeNeo4J.getRelationships(txnNeo4J, GraphDirection.Outgoing, ENTER_PLATFORM).toList();
        List<GraphRelationship> relationshipsInMem = nodeInMem.getRelationships(txnInMem, GraphDirection.Outgoing, ENTER_PLATFORM).toList();

        assertEquals(relationshipsNeo4J.size(), relationshipsInMem.size());

        checkRelationshipsMatch(relationshipsNeo4J, relationshipsInMem);

    }

    @Test
    void shouldHaveRelationshipsAtStationNodes() {
        checkForType(stationRepository.getStations());
    }

    @Test
    void shouldHaveRelationshipsAtPlatformNodes() {
        PlatformRepository platformRepository = componentContainerInMemory.get(PlatformRepository.class);
        checkForType(platformRepository.getPlatforms(EnumSet.of(TransportMode.Tram)));
    }

    @Disabled("slow...")
    @Test
    void shouldHaveRelationshipsAtRouteStationNodes() {
        checkForType(stationRepository.getRouteStations());
    }

    @Test
    void shouldCompareSameJourney() {

        TramDate when = TestEnv.testDay();
        TramTime time = TramTime.of(17,45);
        EnumSet<TransportMode> requestedModes = EnumSet.of(TransportMode.Tram);
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 1,
                maxJourneyDuration, maxNumResults, requestedModes);

        TramStations begin = Altrincham;
        TramStations dest = OldTrafford;

        List<Journey> journeysInMem = calculatorInMem.calculateRouteAsList(begin, dest, journeyRequest);
        assertFalse(journeysInMem.isEmpty(), journeyRequest.toString());

        List<Journey> journeysNeo4J = calculatorNeo4J.calculateRouteAsList(begin, dest, journeyRequest);
        assertFalse(journeysNeo4J.isEmpty(), journeyRequest.toString());

        assertEquals(journeysNeo4J, journeysInMem);
    }

    private <T extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> void checkForType(Collection<T> items) {
        for(final T item : items) {
            final GraphNode inMemoryNode = txnInMem.findNode(item);
            final GraphNode neo4JNode = txnNeo4J.findNode(item);
            assertEquals(neo4JNode.getLabels(), inMemoryNode.getLabels());

            checkProps(neo4JNode, inMemoryNode);

            checkSameDirections(neo4JNode, inMemoryNode, GraphDirection.Outgoing);
            checkSameDirections(neo4JNode, inMemoryNode, GraphDirection.Incoming);
        }
    }

    private void checkSameDirections(final GraphNode graphNodeA, final GraphNode graphNodeB, final GraphDirection direction) {
        final List<GraphRelationship> relationshipsA = graphNodeA.getRelationships(txnNeo4J, direction).toList();
        final List<GraphRelationship> relationshipsB = graphNodeB.getRelationships(txnInMem, direction).toList();

        assertEquals(relationshipsA.size(), relationshipsB.size());

        for(final TransportRelationshipTypes type : TransportRelationshipTypes.values()) {
            long countA = relationshipsA.stream().filter(relationship -> relationship.isType(type)).count();
            long countB = relationshipsB.stream().filter(relationship -> relationship.isType(type)).count();
            assertEquals(countA, countB);
        }

        for(final GraphRelationship relationshipA : relationshipsA) {
            IdFor<? extends CoreDomain> beginId = relationshipA.getStart(txnNeo4J);
            IdFor<? extends CoreDomain> endId = relationshipA.getEnd(txnNeo4J);

            List<GraphRelationship> beingAndEndMatch = relationshipsB.stream().
                    filter(relationship -> relationship.getStart(txnInMem).equals(beginId)).
                    filter(relationship -> relationship.getEnd(txnInMem).equals(endId)).toList();

            assertFalse(beingAndEndMatch.isEmpty());

            TransportRelationshipTypes typeA = relationshipA.getType();

            List<GraphRelationship> typeMatches = beingAndEndMatch.stream().
                    filter(relationship -> relationship.isType(typeA)).toList();
            assertFalse(typeMatches.isEmpty());

            final Map<String, Object> expectedProps = relationshipA.getAllProperties();
            final Set<GraphRelationship> match = typeMatches.stream().
                    filter(graphRelationship -> matchProps(expectedProps, graphRelationship)).
                    collect(Collectors.toSet());
            assertEquals(1, match.size(),"mismatch for " + expectedProps + " from " + typeMatches + " for relationship " + relationshipA);

        }

    }

    private void checkRelationshipsMatch(final List<GraphRelationship> listA, final List<GraphRelationship> listB) {
        for(final GraphRelationship expected : listA) {
            final IdFor<? extends CoreDomain> startId = expected.getStart(txnNeo4J);
            final IdFor<? extends CoreDomain> endId = expected.getEnd(txnNeo4J);

            final List<GraphRelationship> beginAndEndMatch = listB.stream().
                    filter(graphRelationship -> graphRelationship.getStart(txnInMem).equals(startId)).
                    filter(graphRelationship -> graphRelationship.getEnd(txnInMem).equals(endId)).
                    toList();
            assertFalse(beginAndEndMatch.isEmpty(), "Did not match begin " + startId + " and end " + endId + " for any of " + listB);

            final Map<String, Object> expectedProps = expected.getAllProperties();
            final Set<GraphRelationship> match = beginAndEndMatch.stream().
                    filter(graphRelationship -> matchProps(expectedProps, graphRelationship)).
                    collect(Collectors.toSet());
            assertEquals(1, match.size(),"mismatch for " + expectedProps + " from " + beginAndEndMatch + " for relationship " + expected);
        }
    }

    private boolean matchProps(final Map<String, Object> expected, final GraphRelationship graphRelationship) {
        final Map<String, Object> props = graphRelationship.getAllProperties();

        if (!expected.keySet().equals(props.keySet())) {
            return false;
        }

        for(final String key : expected.keySet()) {
            if (key.equals(TRANSPORT_MODES.getText())) {
                if  (!checkModes(expected, props)) {
                    return false;
                }
            } else if (key.equals(TRIP_ID_LIST.getText())) {
                if (!checkTrips(expected, props)) {
                    return false;
                }
            } else {
                if (!expected.get(key).equals(props.get(key))) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkTrips(final Map<String, Object> expected, final Map<String, Object> props) {
        final String[] arrayA = (String[]) expected.get(TRIP_ID_LIST.getText());
        final String[] arrayB = (String[]) props.get(TRIP_ID_LIST.getText());
        if (arrayA.length!=arrayB.length) {
            return false;
        }

        final Set<String> setA = new HashSet<>(Arrays.asList(arrayA));
        final Set<String> setB = new HashSet<>(Arrays.asList(arrayB));

        return setA.equals(setB);
    }

    private static boolean checkModes(final Map<String, Object> expected, final Map<String, Object> props) {
        final short[] arrayA = (short[]) expected.get(TRANSPORT_MODES.getText());
        final short[] arrayB = (short[]) props.get(TRANSPORT_MODES.getText());
        if (arrayA.length!=arrayB.length) {
            return false;
        }
        for (int i = 0; i < arrayA.length; i++) {
            if (arrayA[i]!=arrayB[i]) {
                return false;
            }
        }

        return true;
    }


    private void checkProps(final GraphEntity graphEntityA, final GraphEntity graphEntityB) {
        final Map<String, Object> propsA = graphEntityA.getAllProperties();
        final Map<String, Object> propsB = graphEntityB.getAllProperties();

        assertEquals(propsA.size(), propsB.size());

        for(final String key : propsA.keySet()) {
            assertTrue(propsB.containsKey(key));
            if (key.equals(TRANSPORT_MODES.getText())) {
                final short[] arrayA = (short[]) propsA.get(key);
                final short[] arrayB = (short[]) propsB.get(key);
                assertArrayEquals(arrayA, arrayB, "mismatch on " + key + " for " + graphEntityA + " and " + graphEntityB);
            } else {
                assertEquals(propsA.get(key), propsB.get(key), "mismatch on " + key + " for " + graphEntityA + " and " + graphEntityB);
            }
        }

    }
}
