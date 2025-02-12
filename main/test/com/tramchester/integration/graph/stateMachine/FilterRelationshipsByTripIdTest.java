package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.stateMachine.FilterRelationshipsByTripId;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithGroupsEnabled;
import com.tramchester.repository.RouteRepository;
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

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.*;

public class FilterRelationshipsByTripIdTest {
    private static GuiceContainerDependencies componentContainer;
    private MutableGraphTransaction txn;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
    private TramDate when;

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

        componentContainer.get(StagedTransportGraphBuilder.Ready.class);

        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTxMutable();

        when = TestEnv.testDay();

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
    void shouldFindRelationshipsTowardsDestination() {
        Station station = NavigationRoad.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(KnownTramRoute.getBuryManchesterAltrincham(when), when);

        ImmutableGraphNode node = findRouteStation(station, route);

        List<Trip> callingTrips = route.getTrips().stream().
                filter(trip -> trip.callsAt(station.getId())).
                filter(trip -> !trip.lastStation().equals(station.getId())).
                filter(trip -> trip.isAfter(station.getId(), Altrincham.getId())).
                toList();

        assertTrue(callingTrips.size()>1);

        IdFor<Trip> tripId = callingTrips.get(0).getId();

        FilterRelationshipsByTripId filter = new FilterRelationshipsByTripId(tripId);

        List<ImmutableGraphRelationship> results = filter.apply(txn, node).toList();

        assertEquals(1, results.size());

        // should have a corresponding inbound relationship for the trip id

        Stream<ImmutableGraphRelationship> inbounds = node.getRelationships(txn, Direction.INCOMING, TransportRelationshipTypes.TRAM_GOES_TO);

        List<ImmutableGraphRelationship> matchingInbound = inbounds.filter(inbound -> inbound.getTripId().equals(tripId)).toList();

        assertEquals(1, matchingInbound.size());
    }

}
