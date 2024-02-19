package com.tramchester.unit.graph.diagnostics;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.diagnostics.HowIGotHere;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.graph.search.diagnostics.ServiceReason;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceReasonsTest extends EasyMockSupport {

    private ServiceReasons serviceReasons;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        TramTime time = TramTime.of(13, 45);
        JourneyRequest journeyRequest = new JourneyRequest(TramDate.of(2024,5,30), time,
                false, 3, Duration.ofHours(1), 1, TramsOnly);
        ProvidesNow providesLocalNow = new ProvidesLocalNow();

        serviceReasons = new ServiceReasons(journeyRequest, time, providesLocalNow);

    }

    @Test
    void shouldIncrementTotalChecked() {

        assertEquals(0, serviceReasons.getTotalChecked());

        serviceReasons.incrementTotalChecked();
        serviceReasons.incrementTotalChecked();
        serviceReasons.incrementTotalChecked();

        assertEquals(3, serviceReasons.getTotalChecked());

        serviceReasons.logCounters();

    }

    @Test
    void shouldRecordArrival() {

        Map<ReasonCode, Integer> reasons = serviceReasons.getReasons();

//        assertFalse(reasons.containsKey(ReasonCode.Arrived));

        serviceReasons.recordArrived();
        serviceReasons.recordArrived();
        serviceReasons.recordArrived();

        reasons = serviceReasons.getReasons();

        assertEquals(3, reasons.get(ReasonCode.Arrived));

    }

    @Test
    void shouldRecordReasons() {

        HowIGotHere howIGotHereA = createMock(HowIGotHere.class);
        HowIGotHere howIGotHereB = createMock(HowIGotHere.class);

        Map<ReasonCode, Integer> reasons = serviceReasons.getReasons();

        GraphNodeId idA = GraphNodeId.TestOnly(42);
        GraphNodeId idB = GraphNodeId.TestOnly(98);

        EasyMock.expect(howIGotHereA.getEndNodeId()).andStubReturn(idA);
        EasyMock.expect(howIGotHereB.getEndNodeId()).andStubReturn(idB);


//        assertFalse(reasons.containsKey(ReasonCode.TookTooLong));

        TramTime time = TramTime.of(18,35);

        replayAll();
        serviceReasons.recordReason(ServiceReason.TookTooLong(time, howIGotHereA));
        serviceReasons.recordReason(ServiceReason.TookTooLong(time, howIGotHereA));
        serviceReasons.recordReason(ServiceReason.TookTooLong(time, howIGotHereA));
        serviceReasons.recordReason(ServiceReason.StationClosed(howIGotHereB, TramStations.Shudehill.getId()));

        verifyAll();

        reasons = serviceReasons.getReasons();

        assertEquals(3, reasons.get(ReasonCode.TookTooLong));
        assertEquals(1, reasons.get(ReasonCode.StationClosed));

        Map<GraphNodeId, Integer> nodeVisits = serviceReasons.getNodeVisits();

        assertEquals(2, nodeVisits.size());

        assertEquals(3, nodeVisits.get(idA));
        assertEquals(1, nodeVisits.get(idB));

        serviceReasons.logCounters();

    }

    @Test
    void shouldRecordStates() {

        ImmutableJourneyState state = createMock(ImmutableJourneyState.class);
        EasyMock.expect(state.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformStationState);
        EasyMock.expect(state.getTransportMode()).andStubReturn(TransportMode.Tram);

        replayAll();
        serviceReasons.recordState(state);
        serviceReasons.recordState(state);
        serviceReasons.recordState(state);
        verifyAll();

        Map<TraversalStateType, Integer> states = serviceReasons.getStates();

        assertTrue(states.containsKey(TraversalStateType.PlatformStationState));

        assertEquals(3, states.get(TraversalStateType.PlatformStationState));

        Map<ReasonCode, Integer> reasons = serviceReasons.getReasons();

        assertTrue(reasons.containsKey(ReasonCode.OnTram));

        assertEquals(3, reasons.get(ReasonCode.OnTram));
    }
}
