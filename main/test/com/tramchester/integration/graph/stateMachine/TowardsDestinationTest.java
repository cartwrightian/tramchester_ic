package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.stateMachine.FilterByDestinations;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithGroupsEnabled;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class TowardsDestinationTest {
    private static GuiceContainerDependencies componentContainer;
    private ImmutableGraphTransaction txn;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
    private TramDate when;
    private StationGroupsRepository groupRepository;

    // NOTE: currently (3/2024) most tram stations are not allocated to a local area in Naptan
    // See StationGroupRepositoryTest

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        final TramchesterConfig config = new IntegrationTramTestConfigWithGroupsEnabled();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);

        groupRepository = componentContainer.get(StationGroupsRepository.class);

        componentContainer.get(StagedTransportGraphBuilder.Ready.class);

        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();

        when = TestEnv.testDay();
    }

    @AfterEach
    void onceAfterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveDestinationIds() {

        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.getBuryManchesterAltrincham(when), when);

        ImmutableGraphNode node = findRouteStation(station, route);

        List<ImmutableGraphRelationship> departs = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.DEPART).toList();

        assertFalse(departs.isEmpty());

        LocationCollection destinations = LocationSet.singleton(station);
        //TowardsDestination towardsDestination = new TowardsDestination((station));

        departs.forEach(depart -> {
            LocationId<?> locationId = depart.getLocationId(); // towardsDestination.getLocationIdFor(depart);
            assertTrue(destinations.contains(locationId));
        });

    }

    @NotNull
    private ImmutableGraphNode findRouteStation(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        assertNotNull(routeStation);

        ImmutableGraphNode node = txn.findNode(routeStation);

        assertNotNull(node);
        return node;
    }

    @Test
    void shouldHaveDestinationForStationGroup() {

        Station station = StPetersSquare.from(stationRepository);

        StationLocalityGroup stationGroup = getStationGroup(station);

        LocationCollection destinations = MixedLocationSet.singleton(stationGroup);

        ImmutableGraphNode node = txn.findNode(station);

        assertNotNull(node);

        List<ImmutableGraphRelationship> towardsGroup = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.GROUPED_TO_PARENT).toList();

        assertFalse(towardsGroup.isEmpty());

        towardsGroup.forEach(relationship -> {
            LocationId<?> locationId = relationship.getLocationId();
            assertTrue(destinations.contains(locationId));
        });
    }

    @Test
    void shouldFindRelationshipsTowardsDestination() {
        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.getBuryManchesterAltrincham(when), when);

        ImmutableGraphNode node = findRouteStation(station, route);

        TowardsDestination towardsDestination = new TowardsDestination((station));

        FilterByDestinations<ImmutableGraphRelationship> towards = towardsDestination.fromRouteStation(txn, node);

        assertFalse(towards.isEmpty());
        List<ImmutableGraphRelationship> results = towards.stream().toList();

        // 2 platforms
        assertEquals(2, results.size());

        results.forEach(result -> {
            GraphNode endNode = result.getEndNode(txn);

            assertTrue(endNode.getLabels().contains(GraphLabel.PLATFORM));

            assertEquals(station.getId(), endNode.getStationId());
        });

    }

    @Test
    void shouldFindNoRelationshipsIfNotTowardsDestination() {
        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.getBuryManchesterAltrincham(when), when);

        ImmutableGraphNode node = findRouteStation(station, route);

        TowardsDestination towardsDestination = new TowardsDestination((Bury.from(stationRepository)));

        FilterByDestinations<ImmutableGraphRelationship> towards = towardsDestination.fromRouteStation(txn, node);

        assertTrue(towards.isEmpty());

    }

    @Test
    void shouldFindRelationshipsTowardsDestinationGroupFromStation() {
        Station station = StPetersSquare.from(stationRepository);

        StationLocalityGroup stationGroup = getStationGroup(station);

        ImmutableGraphNode node = txn.findNode(station);

        assertNotNull(node);

        TowardsDestination towardsDestination = new TowardsDestination((stationGroup));

        FilterByDestinations<ImmutableGraphRelationship> towards = towardsDestination.fromStation(txn, node);

        assertFalse(towards.isEmpty());

        Stream<ImmutableGraphRelationship> results = towards.stream();

        results.forEach(relationship -> {
            GraphNode endNode = relationship.getEndNode(txn);
            assertEquals(stationGroup.getId(), endNode.getStationGroupId());
        });
    }

    @Test
    void shouldFindRelationshipsTowardsDestinationGroupFromRouteStation() {
        Station station = StPetersSquare.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.getBuryManchesterAltrincham(when), when);

        StationLocalityGroup stationGroup = getStationGroup(station);

        @NotNull ImmutableGraphNode node = findRouteStation(station, route);

        TowardsDestination towardsDestination = new TowardsDestination(stationGroup);

        FilterByDestinations<ImmutableGraphRelationship> towards = towardsDestination.fromRouteStation(txn, node);

        assertFalse(towards.isEmpty());

        Stream<ImmutableGraphRelationship> results = towards.stream();

        results.forEach(relationship -> {
            GraphNode endNode = relationship.getEndNode(txn);
            IdFor<Station> endNodeStationId = endNode.getStationId();
            Station endNodeStation = stationRepository.getStationById(endNodeStationId);
            assertEquals(station.getLocalityId(), endNodeStation.getLocalityId());
        });
    }

//    @Test
//    void shouldProvideOriginalDestinationsNotExpanded() {
//        Station station = StPetersSquare.from(stationRepository);
//
//        StationLocalityGroup stationGroup = getStationGroup(station);
//
//        TowardsDestination towardsDestination = new TowardsDestination(stationGroup);
//
//        LocationCollection results = towardsDestination.getDestinations();
//
//        assertEquals(1, results.size());
//
//        Location<?> result = results.locationStream().toList().getFirst();
//
//        assertEquals(stationGroup, result);
//    }

    @Test
    void shouldNotFindRelationshipsIfNotTowardsDestinationGroup() {
        Station station = StPetersSquare.from(stationRepository);

        ImmutableGraphNode node = txn.findNode(station);

        assertNotNull(node);

        TowardsDestination towardsDestination = new TowardsDestination((Bury.from(stationRepository)));

        FilterByDestinations<ImmutableGraphRelationship> results = towardsDestination.fromStation(txn, node);

        assertTrue(results.isEmpty());

    }

    private StationLocalityGroup getStationGroup(Station station) {
        IdFor<NPTGLocality> localityId = station.getLocalityId();
        return groupRepository.getStationGroupForArea(localityId);
    }
}
