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
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Direction;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class TowardsDestinationTest {
    private static GuiceContainerDependencies componentContainer;
    private MutableGraphTransaction txn;
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
        txn = database.beginTxMutable();

        when = TestEnv.testDay();

    }

    @Test
    void shouldHaveDestinationIds() {

        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, when);

        ImmutableGraphNode node = findRouteStation(station, route);

        List<ImmutableGraphRelationship> departs = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.DEPART).toList();

        assertFalse(departs.isEmpty());

        LocationCollection destinations = LocationSet.singleton(station);
        TowardsDestination towardsDestination = new TowardsDestination((station));

        departs.forEach(depart -> {
            LocationId locationId = towardsDestination.getLocationIdFor(depart);
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

        StationGroup stationGroup = getStationGroup(station);

        LocationCollection destinations = MixedLocationSet.singleton(stationGroup);

        ImmutableGraphNode node = txn.findNode(station);

        assertNotNull(node);

        List<ImmutableGraphRelationship> towardsGroup = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.GROUPED_TO_PARENT).toList();

        assertFalse(towardsGroup.isEmpty());

        TowardsDestination towardsDestination = new TowardsDestination((stationGroup));

        towardsGroup.forEach(relationship -> {
            LocationId locationId = towardsDestination.getLocationIdFor(relationship);
            assertTrue(destinations.contains(locationId));
        });
    }

    @Test
    void shouldFindRelationshipsTowardsDestination() {
        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, when);

        ImmutableGraphNode node = findRouteStation(station, route);

        TowardsDestination towardsDestination = new TowardsDestination((station));

        Stream<ImmutableGraphRelationship> departs = node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        List<ImmutableGraphRelationship> results = towardsDestination.getTowardsDestination(departs).stream().toList();

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
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, when);

        ImmutableGraphNode node = findRouteStation(station, route);

        TowardsDestination towardsDestination = new TowardsDestination((Bury.from(stationRepository)));

        Stream<ImmutableGraphRelationship> departs = node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        List<ImmutableGraphRelationship> results = towardsDestination.getTowardsDestination(departs).stream().toList();

        assertEquals(0, results.size());
    }

    @Test
    void shouldFindRelationshipsTowardsDestinationGroup() {
        Station station = StPetersSquare.from(stationRepository);

        StationGroup stationGroup = getStationGroup(station);

        ImmutableGraphNode node = txn.findNode(station);

        assertNotNull(node);

        Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.GROUPED_TO_PARENT);

        TowardsDestination towardsDestination = new TowardsDestination((stationGroup));

        FilterByDestinations<ImmutableGraphRelationship> results = towardsDestination.getTowardsDestination(relationships);

        assertFalse(results.isEmpty());

        results.forEach(relationship -> {
            GraphNode endNode = relationship.getEndNode(txn);
            assertEquals(stationGroup.getId(), endNode.getStationGroupId());
        });
    }

    @Test
    void shouldNotFindRelationshipsIfNotTowardsDestinationGroup() {
        Station station = StPetersSquare.from(stationRepository);

        ImmutableGraphNode node = txn.findNode(station);

        assertNotNull(node);

        Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.GROUPED_TO_PARENT);

        TowardsDestination towardsDestination = new TowardsDestination((Bury.from(stationRepository)));

        FilterByDestinations<ImmutableGraphRelationship> results = towardsDestination.getTowardsDestination(relationships);

        assertTrue(results.isEmpty());

    }

    private StationGroup getStationGroup(Station station) {
        IdFor<NPTGLocality> localityId = station.getLocalityId();
        return groupRepository.getStationGroupForArea(localityId);
    }
}
