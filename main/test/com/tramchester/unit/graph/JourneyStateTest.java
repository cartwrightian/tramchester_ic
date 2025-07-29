package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.neo4j.GraphNodeIdNeo4J;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static org.junit.jupiter.api.Assertions.*;

class JourneyStateTest extends EasyMockSupport {

    private TramTime queryTime;
    private NotStartedState traversalState;
    private GraphNode node;

    // TODO ON BUS

    @BeforeEach
    void onceBeforeEachTestRuns() {
        TramDate queryDate = TestEnv.testDay();

        node = EasyMock.createMock(GraphNode.class);

        MutableStation station = StationHelper.forTestMutable("destinationStationId", "area", "name", new LatLong(1,1),
                DataSourceID.tfgm, false);

        station.addRoutePickUp(TestEnv.getTramTestRoute());
        station.addRoutePickUp(TestEnv.getTramTestRoute());

        final TramchesterConfig config = TestEnv.GET();

        queryTime = TramTime.of(9, 15);

        TowardsDestination towardsDestination = new TowardsDestination(station);
        StateBuilderParameters builderParams = new StateBuilderParameters(queryDate, queryTime,
                towardsDestination, config, TramsOnly);
        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParams);

        MutableGraphTransactionNeo4J txn = createMock(MutableGraphTransactionNeo4J.class);

        GraphNodeId startNodeId = GraphNodeIdNeo4J.TestOnly(88842L);

        traversalState = new NotStartedState(traversalStateFactory, startNodeId, txn);
    }

    @Test
    void shouldBeginJourney() {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));

        Duration currentCost = Duration.ZERO;
        state.updateTotalCost(currentCost);
        assertEquals(queryTime, state.getJourneyClock());
        assertFalse(TransportMode.isTram(state));

        currentCost = Duration.ofMinutes(14);
        state.updateTotalCost(currentCost);
        assertEquals(TramTime.of(9,29), state.getJourneyClock());
        assertFalse(TransportMode.isTram(state));

    }

    @Test
    void shouldBoardATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));
        assertFalse(state.hasBegunJourney());

        Duration currentCost = Duration.ofMinutes(10);
        TramTime boardingTime = TramTime.of(9, 30);
        state.board(TransportMode.Tram, node, true);
        assertTrue(state.hasBegunJourney());
        state.recordTime(boardingTime,currentCost);

        assertTrue(TransportMode.isTram(state));
        assertEquals(boardingTime, state.getJourneyClock());
    }

    @Test
    void shouldConnection() {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));

        state.beginWalk(node, true, Duration.ofMinutes(42));
        assertEquals(1, state.getNumberWalkingConnections());

        state.beginWalk(node, true, Duration.ofMinutes(42));
        assertEquals(2, state.getNumberWalkingConnections());

        state.endWalk(node);
        assertEquals(2, state.getNumberWalkingConnections());
    }

    @Test
    void shouldNotBoardATramIfAlreadyOnATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        state.board(TransportMode.Tram, node, true);
        assertThrows(TramchesterException.class, () -> state.board(TransportMode.Tram, node, true));
    }

    @Test
    void shouldNotLeaveATramIfAlreadyOffATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        TramTime boardingTime = TramTime.of(9, 30);
        IdFor<Trip> tripId = MutableTrip.createId("trip1");

        Duration currentCost = Duration.ofMinutes(14);
        state.board(TransportMode.Tram, node, true);
        assertTrue(state.hasBegunJourney());

        state.recordTime(boardingTime,currentCost);
        state.beginTrip(tripId);
        state.leave(TransportMode.Tram, Duration.ofMinutes(20), node);
        assertTrue(state.hasBegunJourney());

        assertThrows(TramchesterException.class, () -> state.leave(TransportMode.Tram, Duration.ofMinutes(25), node));
    }

    @Test
    void shouldHaveCorrectClockDuringATrip() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        TramTime boardingTime = TramTime.of(9, 30);
        state.board(TransportMode.Tram, node, true);
        state.recordTime(boardingTime,Duration.ofMinutes(10));
        assertEquals(boardingTime, state.getJourneyClock());

        state.updateTotalCost(Duration.ofMinutes(15)); // 15 - 10
        assertEquals(boardingTime.plusMinutes(5), state.getJourneyClock());

        state.updateTotalCost(Duration.ofMinutes(20));  // 20 - 10
        assertEquals(boardingTime.plusMinutes(10), state.getJourneyClock());
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));
        IdFor<Trip> tripId = MutableTrip.createId("trip1");

        state.board(TransportMode.Tram, node, true);
        state.recordTime(TramTime.of(9,30),Duration.ofMinutes(10));         // 10 mins cost
        state.beginTrip(tripId);
        assertTrue(TransportMode.isTram(state));
        assertFalse(state.alreadyDeparted(tripId));

        state.leave(TransportMode.Tram, Duration.ofMinutes(25), node);                            // 25 mins cost, offset is 15 mins
        assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time
        assertFalse(TransportMode.isTram(state));
        assertTrue(state.alreadyDeparted(tripId));

        state.updateTotalCost(Duration.ofMinutes(35));
        assertEquals(TramTime.of(9,55), state.getJourneyClock()); // i.e not just queryTime + 35 minutes
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingAndBoardingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        IdFor<Trip> tripId1 = MutableTrip.createId("trip1");
        IdFor<Trip> tripId2 = MutableTrip.createId("trip2");

        state.board(TransportMode.Tram, node, true);
        state.recordTime(TramTime.of(9,30),Duration.ofMinutes(10));         // 10 mins cost
        state.beginTrip(tripId1);
        assertFalse(state.alreadyDeparted(tripId1));

        state.leave(TransportMode.Tram, Duration.ofMinutes(25), node);                            // 25 mins cost, offset is 15 mins
        assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time
        assertTrue(state.alreadyDeparted(tripId1));
        assertFalse(state.alreadyDeparted(tripId2));

        state.board(TransportMode.Tram, node, true);
        state.recordTime(TramTime.of(9,50),Duration.ofMinutes(25));
        state.beginTrip(tripId2);
        assertEquals(TramTime.of(9,50), state.getJourneyClock()); // should be depart tram time

        state.leave(TransportMode.Tram, Duration.ofMinutes(35), node);                            // 35-25 = 10 mins
        assertEquals(TramTime.of(10,0), state.getJourneyClock());

        assertTrue(state.alreadyDeparted(tripId1));
        assertTrue(state.alreadyDeparted(tripId2));
    }

    @Test
    void shouldCreateNewState() throws TramchesterException {
        JourneyState journeyState = new JourneyState(TramTime.of(7,55), traversalState);
        journeyState.beginWalk(node, true, Duration.ofMinutes(42));
        IdFor<Trip> tripId = MutableTrip.createId("trip1");

        JourneyState newStateA = JourneyState.fromPrevious(journeyState);
        assertEquals(TramTime.of(7,55), journeyState.getJourneyClock());
        assertFalse(TransportMode.isTram(newStateA));
        assertFalse(newStateA.hasBegunJourney());
        assertEquals(0, newStateA.getNumberChanges());
        assertEquals(1, newStateA.getNumberWalkingConnections());

        newStateA.board(TransportMode.Tram, node, true);
        newStateA.recordTime(TramTime.of(8,15), Duration.ofMinutes(15));
        newStateA.beginTrip(tripId);
        assertEquals(TramTime.of(8,15), newStateA.getJourneyClock());
        newStateA.beginWalk(node, true, Duration.ofMinutes(42));

        JourneyState newStateB = JourneyState.fromPrevious(newStateA);
        assertTrue(newStateB.hasBegunJourney());

        assertEquals(TramTime.of(8,15), newStateB.getJourneyClock());
        assertTrue(TransportMode.isTram(newStateB));
        assertEquals(tripId, newStateB.getCurrentTrip());
        newStateB.leave(TransportMode.Tram, Duration.ofMinutes(20), node);
        newStateB.board(TransportMode.Tram, node, true);
        assertEquals(2, newStateB.getNumberWalkingConnections());
        assertEquals(1, newStateB.getNumberChanges());
    }

}
