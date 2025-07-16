package com.tramchester.unit.graph.diagnostics;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.GraphNodeId;
import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
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
    private CreateJourneyDiagnostics failedJourneyDiagnostics;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        TramTime time = TramTime.of(13, 45);
        JourneyRequest journeyRequest = new JourneyRequest(TramDate.of(2024,5,30), time,
                false, 3, Duration.ofHours(1), 1, TramsOnly);
        providesLocalNow = new ProvidesLocalNow();

        failedJourneyDiagnostics = createMock(CreateJourneyDiagnostics.class);

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

        Station dest = TramStations.Piccadilly.fake();
        LocationCollection destinations = LocationCollectionSingleton.of(dest);

        journeyRequest.setDiag(true);

        serviceReasons = new ServiceReasons(journeyRequest, time, providesLocalNow, failedJourneyDiagnostics);

        final HowIGotHere howIGotHere = createMock(HowIGotHere.class);

        GraphNodeId nodeId = GraphNodeId.TestOnly(42);
        EasyMock.expect(howIGotHere.getEndNodeId()).andStubReturn(nodeId);

        ImmutableGraphNode node = createMock(ImmutableGraphNode.class);
        EasyMock.expect(node.getLabels()).andStubReturn(EnumSet.of(GraphLabel.STATION));
        EasyMock.expect(node.getAllProperties()).andStubReturn(new HashMap<>());
        EasyMock.expect(node.getId()).andStubReturn(nodeId);

        GraphTransaction txn = createMock(ImmutableGraphTransactionNeo4J.class);
        EasyMock.expect(txn.getNodeById(nodeId)).andReturn(node);

        RouteCalculatorSupport.PathRequest pathRequest = createMock(RouteCalculatorSupport.PathRequest.class);
        EasyMock.expect(pathRequest.getNumChanges()).andReturn(3);

        HeuristicsReason serviceReasonA = HeuristicsReasons.AlreadyDeparted(TramTime.of(15, 33), howIGotHere);
        HeuristicsReason serviceReasonB = HeuristicsReasons.DoesNotOperateAtHour(TramTime.of(16,32),
                howIGotHere, 13);

        List<StationDiagnosticsDTO> dtos = new ArrayList<>();
        List<LocationRefWithPosition> destinationDTOs = Arrays.asList(new LocationRefWithPosition(dest));
        JourneyDiagnostics someDiags = new JourneyDiagnostics(dtos, destinationDTOs, 1, 1);
        List<HeuristicsReason> reasons = Arrays.asList(serviceReasonA, serviceReasonB);
        EasyMock.expect(failedJourneyDiagnostics.recordFailedJourneys(reasons, destinations)).andReturn(someDiags);

        replayAll();
        serviceReasons.recordVisit(howIGotHere);
        serviceReasons.recordReason(serviceReasonA);
        serviceReasons.recordReason(serviceReasonB);
        Map<GraphNodeId, Integer> nodeVisits = serviceReasons.getNodeVisits();

        serviceReasons.reportReasons(txn, pathRequest, destinations);
        verifyAll();


        assertEquals(1, nodeVisits.get(nodeId), nodeVisits.toString());

    }

    @Test
    void shouldRecordArrival() {

        HowIGotHere howIGotHere = createMock(HowIGotHere.class);

        Duration totalCostSoFar = Duration.ofMinutes(42);
        serviceReasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere, totalCostSoFar,2));
        serviceReasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere, totalCostSoFar, 2));
        serviceReasons.recordReason(HeuristicReasonsOK.Arrived(howIGotHere, totalCostSoFar, 2));

        Map<ReasonCode, Integer> reasons = serviceReasons.getReasons();

        assertEquals(3, reasons.get(ReasonCode.Arrived));

    }

    @Test
    void shouldRecordReasons() {

        HowIGotHere howIGotHereA = createMock(HowIGotHere.class);
        HowIGotHere howIGotHereB = createMock(HowIGotHere.class);


        GraphNodeId idA = GraphNodeId.TestOnly(42);
        GraphNodeId idB = GraphNodeId.TestOnly(98);

        EasyMock.expect(howIGotHereA.getEndNodeId()).andStubReturn(idA);
        EasyMock.expect(howIGotHereB.getEndNodeId()).andStubReturn(idB);

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
