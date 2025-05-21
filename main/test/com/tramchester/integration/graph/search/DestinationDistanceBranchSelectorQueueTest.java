package com.tramchester.integration.graph.search;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.LocationDistances;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.selectors.DestinationDistanceBranchSelector;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.traversal.TraversalBranch;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertSame;

public class DestinationDistanceBranchSelectorQueueTest extends EasyMockSupport {

    private static GuiceContainerDependencies componentContainer;

    private StationRepository stationRepository;
    private LocationDistances locationDistances;
    private LocationSet<Station> destinations;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {

        TramchesterConfig tramchesterConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        locationDistances = componentContainer.get(LocationDistances.class);
        destinations = getDestinations(Arrays.asList(TramStations.Victoria, TramStations.Shudehill));
    }

    @Test
    void shouldHaveExpectedGeoOrderDifferentNodes() {
        DestinationDistanceBranchSelector.TraversalBranchQueue traversalBranchQueue =
                new DestinationDistanceBranchSelector.TraversalBranchQueue(locationDistances, destinations);

        ImmutableJourneyState stateA = createMockJourneyState(45, TramStations.Altrincham, true,
                TramTime.of(8, 15));
        ImmutableJourneyState stateB = createMockJourneyState(87, TramStations.Deansgate, true,
                TramTime.of(8, 15));
        ImmutableJourneyState stateC = createMockJourneyState(55, TramStations.NavigationRoad, true,
                TramTime.of(8, 15));

        TraversalBranch branchA = createMockBranchFor(stateA);
        TraversalBranch branchB = createMockBranchFor(stateB);
        TraversalBranch branchC = createMockBranchFor(stateC);

        replayAll();
        traversalBranchQueue.addBranch(branchA);
        traversalBranchQueue.addBranch(branchB);
        traversalBranchQueue.addBranch(branchC);

        assertSame(stateB, traversalBranchQueue.removeFront().state());
        assertSame(stateC, traversalBranchQueue.removeFront().state());
        assertSame(stateA, traversalBranchQueue.removeFront().state());

        verifyAll();
    }

    @Test
    void shouldFallbackToClockIfSameNode() {
        DestinationDistanceBranchSelector.TraversalBranchQueue traversalBranchQueue =
                new DestinationDistanceBranchSelector.TraversalBranchQueue(locationDistances, destinations);

        ImmutableJourneyState stateA = createMockJourneyState(45, TramStations.Deansgate, true, TramTime.of(8,45));
        ImmutableJourneyState stateB = createMockJourneyState(45, TramStations.Deansgate, true, TramTime.of(8, 5));
        ImmutableJourneyState stateC = createMockJourneyState(45, TramStations.Deansgate, true, TramTime.of(8, 25));

        TraversalBranch branchA = createMockBranchFor(stateA);
        TraversalBranch branchB = createMockBranchFor(stateB);
        TraversalBranch branchC = createMockBranchFor(stateC);

        replayAll();
        traversalBranchQueue.addBranch(branchA);
        traversalBranchQueue.addBranch(branchB);
        traversalBranchQueue.addBranch(branchC);

        assertSame(stateB, traversalBranchQueue.removeFront().state());
        assertSame(stateC, traversalBranchQueue.removeFront().state());
        assertSame(stateA, traversalBranchQueue.removeFront().state());

        verifyAll();
    }


    @Test
    void shouldHaveExpectedOrderNotStartedYet() {
        DestinationDistanceBranchSelector.TraversalBranchQueue traversalBranchQueue =
                new DestinationDistanceBranchSelector.TraversalBranchQueue(locationDistances, destinations);

        ImmutableJourneyState stateA = createMockJourneyState(45, TramStations.Altrincham, false, TramTime.of(8, 45));
        ImmutableJourneyState stateB = createMockJourneyState(87, TramStations.Altrincham, false, TramTime.of(8, 5));
        ImmutableJourneyState stateC = createMockJourneyState(55, TramStations.Altrincham, false, TramTime.of(8, 25));

        TraversalBranch branchA = createMockBranchFor(stateA);
        TraversalBranch branchB = createMockBranchFor(stateB);
        TraversalBranch branchC = createMockBranchFor(stateC);

        replayAll();
        traversalBranchQueue.addBranch(branchA);
        traversalBranchQueue.addBranch(branchB);
        traversalBranchQueue.addBranch(branchC);

        assertSame(stateB, traversalBranchQueue.removeFront().state());  // earliest
        assertSame(stateC, traversalBranchQueue.removeFront().state());
        assertSame(stateA, traversalBranchQueue.removeFront().state());

        verifyAll();
    }

    @Test
    void shouldHaveExpectedOrderSomeStarted() {
        DestinationDistanceBranchSelector.TraversalBranchQueue traversalBranchQueue =
                new DestinationDistanceBranchSelector.TraversalBranchQueue(locationDistances, destinations);

        ImmutableJourneyState stateA = createMockJourneyState(45, TramStations.Altrincham, true, TramTime.of(8, 45));
        ImmutableJourneyState stateB = createMockJourneyState(87, TramStations.Deansgate, false, TramTime.of(8, 5));
        ImmutableJourneyState stateC = createMockJourneyState(55, TramStations.NavigationRoad, false, TramTime.of(8, 25));

        TraversalBranch branchA = createMockBranchFor(stateA);
        TraversalBranch branchB = createMockBranchFor(stateB);
        TraversalBranch branchC = createMockBranchFor(stateC);

        replayAll();
        traversalBranchQueue.addBranch(branchA);
        traversalBranchQueue.addBranch(branchB);
        traversalBranchQueue.addBranch(branchC);

        assertSame(stateA, traversalBranchQueue.removeFront().state());
        assertSame(stateB, traversalBranchQueue.removeFront().state());
        assertSame(stateC, traversalBranchQueue.removeFront().state());

        verifyAll();
    }


    @NotNull
    private TraversalBranch createMockBranchFor(ImmutableJourneyState stateA) {
        TraversalBranch branchA = createMock(TraversalBranch.class);
        EasyMock.expect(branchA.state()).andStubReturn(stateA);
        return branchA;
    }

    @NotNull
    private ImmutableJourneyState createMockJourneyState(int nodeId, TramStations station, boolean begunJourney, TramTime tramTime) {
        ImmutableJourneyState state = createMock(ImmutableJourneyState.class);
        EasyMock.expect(state.getNodeId()).andStubReturn(GraphNodeId.TestOnly(nodeId));
        EasyMock.expect(state.hasBegunJourney()).andStubReturn(begunJourney);
        IdFor<Station> stationId = station.getId();
        //EasyMock.expect(state.approxPosition()).andStubReturn(stationId);
        state.approxPosition();
        EasyMock.expectLastCall().andStubReturn(stationId);
        EasyMock.expect(state.getJourneyClock()).andStubReturn(tramTime);
        return state;
    }

    private LocationSet<Station> getDestinations(List<TramStations> list) {
        final Set<Station> asSet = list.stream().map(station -> station.from(stationRepository)).collect(Collectors.toSet());
        return LocationSet.of(asSet);
    }
}
