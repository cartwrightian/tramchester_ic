package com.tramchester.unit.graph.diagnostics;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.RouteCalculatorSupport;
import com.tramchester.graph.search.diagnostics.*;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceReasonsTest extends EasyMockSupport {

    private ServiceReasons serviceReasons;
    private ProvidesLocalNow providesLocalNow;
    private CreateFailedJourneyDiagnostics failedJourneyDiagnostics;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        TramTime time = TramTime.of(13, 45);
        JourneyRequest journeyRequest = new JourneyRequest(TramDate.of(2024,5,30), time,
                false, 3, Duration.ofHours(1), 1, TramsOnly);
        providesLocalNow = new ProvidesLocalNow();

        failedJourneyDiagnostics = createMock(CreateFailedJourneyDiagnostics.class);

        serviceReasons = new ServiceReasons(journeyRequest, time, providesLocalNow, failedJourneyDiagnostics);

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
    void shouldProvideDiagnostics() {
        TramTime time = TramTime.of(13, 45);
        JourneyRequest journeyRequest = new JourneyRequest(TramDate.of(2024,5,30), time,
                false, 3, Duration.ofHours(1), 1, TramsOnly);

        journeyRequest.setDiag(true);

        serviceReasons = new ServiceReasons(journeyRequest, time, providesLocalNow, failedJourneyDiagnostics);

        HowIGotHere howIGotHere = createMock(HowIGotHere.class);

        GraphNodeId nodeId = GraphNodeId.TestOnly(42);
        EasyMock.expect(howIGotHere.getEndNodeId()).andStubReturn(nodeId);

        ImmutableGraphNode node = createMock(ImmutableGraphNode.class);
        EasyMock.expect(node.getLabels()).andStubReturn(EnumSet.of(GraphLabel.STATION));
        EasyMock.expect(node.getAllProperties()).andStubReturn(new HashMap<>());
        EasyMock.expect(node.getId()).andStubReturn(nodeId);

        GraphTransaction txn = createMock(GraphTransaction.class);
        EasyMock.expect(txn.getNodeById(nodeId)).andReturn(node);

        RouteCalculatorSupport.PathRequest pathRequest = createMock(RouteCalculatorSupport.PathRequest.class);
        EasyMock.expect(pathRequest.getNumChanges()).andReturn(3);

        HeuristicsReason serviceReasonA = HeuristicsReasons.AlreadyDeparted(TramTime.of(15, 33), howIGotHere);
        HeuristicsReason serviceReasonB = HeuristicsReasons.DoesNotOperateAtHour(TramTime.of(16,32), howIGotHere);

        List<StationDiagnosticsDTO> dtos = new ArrayList<>();
        JourneyDiagnostics someDiags = new JourneyDiagnostics(dtos, 1, 1);
        List<HeuristicsReason> reasons = Arrays.asList(serviceReasonA, serviceReasonB);
        EasyMock.expect(failedJourneyDiagnostics.recordFailedJourneys(reasons)).andReturn(someDiags);

        replayAll();
        serviceReasons.recordReason(serviceReasonA);
        serviceReasons.recordReason(serviceReasonB);

        serviceReasons.reportReasons(txn, pathRequest);
        verifyAll();
    }

    @Test
    void shouldRecordArrival() {

        HowIGotHere howIGotHere = createMock(HowIGotHere.class);

        serviceReasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere));
        serviceReasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere));
        serviceReasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere));

        Map<ReasonCode, Integer> reasons = serviceReasons.getReasons();

        assertEquals(3, reasons.get(ReasonCode.Arrived));

    }

    @Test
    void shouldRecordReasons() {

        HowIGotHere howIGotHereA = createMock(HowIGotHere.class);
        HowIGotHere howIGotHereB = createMock(HowIGotHere.class);

//        Map<ReasonCode, Integer> reasons = serviceReasons.getReasons();

        GraphNodeId idA = GraphNodeId.TestOnly(42);
        GraphNodeId idB = GraphNodeId.TestOnly(98);

        EasyMock.expect(howIGotHereA.getEndNodeId()).andStubReturn(idA);
        EasyMock.expect(howIGotHereB.getEndNodeId()).andStubReturn(idB);


//        assertFalse(reasons.containsKey(ReasonCode.TookTooLong));

        TramTime time = TramTime.of(18,35);

        replayAll();
        serviceReasons.recordReason(HeuristicsReasons.TookTooLong(time, howIGotHereA));
        serviceReasons.recordReason(HeuristicsReasons.TookTooLong(time, howIGotHereA));
        serviceReasons.recordReason(HeuristicsReasons.TookTooLong(time, howIGotHereA));
        serviceReasons.recordReason(HeuristicsReasons.StationClosed(howIGotHereB, TramStations.Shudehill.getId()));

        verifyAll();

        Map<ReasonCode, Integer> reasons = serviceReasons.getReasons();

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
