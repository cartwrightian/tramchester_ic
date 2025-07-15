package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.GetOutgoingServicesMatchingTripId;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.*;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class TraversalStateTest extends EasyMockSupport {
    private static GuiceContainerDependencies componentContainer;
    private static TramchesterConfig config;
    private GraphTransaction txn;
    private StationRepository stationRepository;
    private TramDate when;
    private TramTime time;
    private Duration cost;
    private TramRouteHelper tramRouteHelper;
    private Station cornbrook;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // need built DB for these tests
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        tramRouteHelper = new TramRouteHelper(componentContainer);
        stationRepository = componentContainer.get(StationRepository.class);

        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();
        when = TestEnv.testDay();

        time = TramTime.of(8,42);
        cost = Duration.ofMinutes(5);

        cornbrook = Cornbrook.from(stationRepository);

    }

    @AfterEach
    void onceAfterEachTest() {
        txn.close();
    }

    @Test
    void shouldHaveDestinationOutboundWhenAvailableOnTrip() {

        TowardsDestination towardsDestination = new TowardsDestination(cornbrook);

        StateBuilderParameters builderParameters = new StateBuilderParameters(when, time,
                towardsDestination, config, TramsOnly);

        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParameters);

        RouteStationStateOnTrip.Builder builder = new RouteStationStateOnTrip.Builder(builderParameters);

        MinuteState minuteState = mockMinuteNode(traversalStateFactory);

        JourneyStateUpdate updateState = new JourneyState(time, minuteState);

        Route route = tramRouteHelper.getGreen(when);

        RouteStation routeStation = new RouteStation(cornbrook, route);

        GraphNode routeStationNode = txn.findNode(routeStation);

        assertNotNull(routeStationNode);

        boolean isInterchange = true;
        Trip trip = findATrip(route, TraffordBar.getId());
        GetOutgoingServicesMatchingTripId filterByTrip = new GetOutgoingServicesMatchingTripId(trip.getId());

        replayAll();
        RouteStationStateOnTrip routeStationStateOnTrip = builder.fromMinuteState(updateState, minuteState, routeStationNode, cost, isInterchange,
                filterByTrip,
                txn);
        verifyAll();

        List<ImmutableGraphRelationship> outbounds = routeStationStateOnTrip.getOutbounds().toList();

        assertEquals(cornbrook.getPlatforms().size(), outbounds.size(), outbounds.toString());

        IdSet<Platform> platformIds = outbounds.stream().map(relationship -> relationship.getEndNode(txn).getPlatformId()).collect(IdSet.idCollector());

        IdSet<Platform> fromStation = cornbrook.getPlatforms().stream().map(HasId::getId).collect(IdSet.idCollector());

        assertEquals(fromStation, platformIds);
    }

    @Test
    void shouldHaveDestinationOutboundWhenAvailableEndTrip() {

        TowardsDestination towardsDestination = new TowardsDestination(cornbrook);
        StateBuilderParameters builderParameters = new StateBuilderParameters(when, time,
                towardsDestination, config, TramsOnly);

        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParameters);

        RouteStationStateEndTrip.Builder builder = new RouteStationStateEndTrip.Builder(builderParameters);

        MinuteState minuteState = mockMinuteNode(traversalStateFactory);

        JourneyStateUpdate updateState = new JourneyState(time, minuteState);

        Route route = tramRouteHelper.getGreen(when);

        RouteStation routeStation = new RouteStation(cornbrook, route);

        GraphNode routeStationNode = txn.findNode(routeStation);

        assertNotNull(routeStationNode, "is the db built?");

        boolean isInterchange = true;

        replayAll();
        RouteStationStateEndTrip routeStationStateOnTrip = builder.fromMinuteState(updateState, minuteState, routeStationNode,
                cost, isInterchange, txn);
        verifyAll();

        List<ImmutableGraphRelationship> outbounds = routeStationStateOnTrip.getOutbounds().toList();

        assertEquals(cornbrook.getPlatforms().size(), outbounds.size(), outbounds.toString());

        IdSet<Platform> platformIds = outbounds.stream().map(relationship -> relationship.getEndNode(txn).getPlatformId()).collect(IdSet.idCollector());

        IdSet<Platform> fromStation = cornbrook.getPlatforms().stream().map(HasId::getId).collect(IdSet.idCollector());

        assertEquals(fromStation, platformIds);
    }

    @Test
    void shouldHaveAllExpectedOutboundWhenDestNotAvailableOnTrip() {

        TowardsDestination towardsDestination = new TowardsDestination(Bury.from(stationRepository));
        StateBuilderParameters builderParameters = new StateBuilderParameters(when, time,
                towardsDestination, config, TramsOnly);

        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParameters);

        RouteStationStateOnTrip.Builder builder = new RouteStationStateOnTrip.Builder(builderParameters);

        MinuteState minuteState = mockMinuteNode(traversalStateFactory);

        Route route = tramRouteHelper.getGreen(when);

        Trip trip = findATrip(route, Deansgate.getId());

        JourneyStateUpdate updateState = new JourneyState(time, minuteState);
        updateState.beginTrip(trip.getId());

        RouteStation routeStation = new RouteStation(cornbrook, route);

        GraphNode routeStationNode = txn.findNode(routeStation);

        assertNotNull(routeStationNode);

        boolean isInterchange = true;

        GetOutgoingServicesMatchingTripId filterByTrip = new GetOutgoingServicesMatchingTripId(trip.getId());

        replayAll();
        RouteStationStateOnTrip routeStationStateOnTrip = builder.fromMinuteState(updateState, minuteState, routeStationNode,
                cost, isInterchange,
                filterByTrip,
                txn);
        verifyAll();

        List<ImmutableGraphRelationship> outbounds = routeStationStateOnTrip.getOutbounds().toList();

        int numberOfPlatforms = cornbrook.getPlatforms().size();

        // todo should actually only be one of the platofmrs??
        assertEquals(numberOfPlatforms +1, outbounds.size(), displayAllProps(outbounds));

        List<ImmutableGraphRelationship> towardsStation = outbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.INTERCHANGE_DEPART)).
                filter(relationship -> relationship.getEndStationId().equals(cornbrook.getId())).
                toList();

        assertEquals(numberOfPlatforms, towardsStation.size(), towardsStation.toString());

        IdFor<Service> serviceId = trip.getService().getId();

        List<ImmutableGraphRelationship> towardsService = outbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                filter(relationship -> relationship.getServiceId().equals(serviceId)).
                toList();

        // routes are bi-driectional, exclude the one that just goes back where we came from
        assertEquals(1, towardsService.size(), displayTowardsService(towardsService));

        ImmutableGraphRelationship towardsTrip = towardsService.getFirst();

        assertTrue(towardsTrip.hasTripIdInList(trip.getId()));
    }

    private String displayAllProps(final List<ImmutableGraphRelationship> relationships) {
        StringBuilder stringBuilder = new StringBuilder();
        relationships.forEach(relationship -> {
            stringBuilder.
                    append(relationship.toString()).
                    //append(relationship.getAllProperties()).append(" ").
                    append(System.lineSeparator());
        });
        return stringBuilder.toString();
    }

    private String displayTowardsService(final List<ImmutableGraphRelationship> towardsService) {
        StringBuilder stringBuilder = new StringBuilder();
        towardsService.forEach(relationship -> {
            stringBuilder.
                    append(relationship.getType()).append(" ").
                    append(relationship.getId()).append(" ").
                    append(relationship.getServiceId()).append(" ").
                    append(relationship.getTripIds()).
                    append(System.lineSeparator());
        });
        return stringBuilder.toString();
    }

    @NotNull
    private Trip findATrip(Route route, IdFor<Station> towardsId) {
        // want a trip that calls at cornbrook but does not finish these
        IdFor<Station> cornbrookId = cornbrook.getId();
        Optional<Trip> findTrip = route.getTrips().stream().
                filter(trip -> trip.callsAt(cornbrookId) && trip.serviceOperatesOn(when)).
                filter(trip -> !trip.lastStation().equals(cornbrookId)).
                filter(trip -> !trip.firstStation().equals(cornbrookId)).
                filter(trip -> trip.isAfter(cornbrookId, towardsId)).
                findFirst();

        assertFalse(findTrip.isEmpty());

        return findTrip.get();
    }

    @NotNull
    private MinuteState mockMinuteNode(TraversalStateFactory traversalStateFactory) {
        MinuteState minuteState = createMock(MinuteState.class);
        //EasyMock.expect(minuteState.getTraversalOps()).andReturn(traversalOps);
        EasyMock.expect(minuteState.getTransaction()).andReturn(txn);
        EasyMock.expect(minuteState.getTraversalStateFactory()).andReturn(traversalStateFactory);
        EasyMock.expect(minuteState.getTotalDuration()).andReturn(Duration.ZERO);
        return minuteState;
    }

}
