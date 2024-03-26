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
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithGroupsEnabled;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Direction;

import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static org.junit.jupiter.api.Assertions.*;

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
        LocationCollection destinations = LocationSet.singleton(station);

        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, when);
        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        assertNotNull(routeStation);

        ImmutableGraphNode node = txn.findNode(routeStation);

        assertNotNull(node);

        List<ImmutableGraphRelationship> departs = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.DEPART).toList();

        assertFalse(departs.isEmpty());

        TowardsDestination towardsDestination = new TowardsDestination((destinations));

        departs.forEach(depart -> {
            LocationId locationId = towardsDestination.getLocationIdFor(depart);
            assertTrue(destinations.contains(locationId));
        });

    }

    @Test
    void shouldHaveDestinationForStationGroup() {

        Station station = StPetersSquare.from(stationRepository);

        ImmutableGraphNode node = txn.findNode(station);

        IdFor<NPTGLocality> localityId = station.getLocalityId();

        StationGroup stationGroup = groupRepository.getStationGroupForArea(localityId);

        LocationCollection destinations = MixedLocationSet.singleton(stationGroup);

        assertNotNull(node);

        List<ImmutableGraphRelationship> towardsGroup = node.getRelationships(txn, Direction.OUTGOING, TransportRelationshipTypes.GROUPED_TO_PARENT).toList();

        assertFalse(towardsGroup.isEmpty());

        TowardsDestination towardsDestination = new TowardsDestination((destinations));


        towardsGroup.forEach(relationship -> {
            LocationId locationId = towardsDestination.getLocationIdFor(relationship);
            assertTrue(destinations.contains(locationId));
        });
    }
}
