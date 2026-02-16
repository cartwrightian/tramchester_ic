package com.tramchester.unit.graph.core;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.diagnostics.*;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static com.tramchester.graph.reference.GraphLabel.*;
import static com.tramchester.graph.search.diagnostics.GraphEvaluationAction.*;
import static com.tramchester.graph.search.diagnostics.ReasonCode.*;
import static com.tramchester.testSupport.reference.TramStations.ExchangeSquare;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramRouteEvaluatorTest extends EasyMockSupport {

    private ServiceHeuristics serviceHeuristics;
    private GraphPath path;
    private HowIGotHere howIGotHere;
    private GraphNode node;
    private ServiceReasons reasons;
    private TramchesterConfig config;
    private PreviousVisits previousSuccessfulVisit;
    private GraphNodeId destinationNodeId;
    private GraphNodeId startNodeId;
    private ArrivalHandler arrivalHandler;
    private ProvidesNow providesNow;
    private TramDuration maxInitialWait;
    private MutableGraphTransaction txn;
    private LocationCollection destinationStations;
    private TramTime queryTime;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        MutableStation forTest = new MutableStation(Station.createId("destinationStationId"),
                NPTGLocality.createId("area"), "name",
                new LatLong(1, 1), new GridPosition(1000,1000), DataSourceID.tfgm,
                false);

        destinationStations = LocationCollectionSingleton.of(forTest);

        forTest.addRouteDropOff(TestEnv.getTramTestRoute());
        forTest.addRouteDropOff(TestEnv.getTramTestRoute());
        forTest.addRoutePickUp(TestEnv.getTramTestRoute());
        forTest.addRoutePickUp(TestEnv.getTramTestRoute());

        arrivalHandler = createMock(ArrivalHandler.class);
        previousSuccessfulVisit = createMock(PreviousVisits.class);
        CreateJourneyDiagnostics failedJourneyDiagnostics = createMock(CreateJourneyDiagnostics.class);

        providesNow = createMock(ProvidesNow.class);

        config = new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(new TFGMGTFSSourceTestConfig(
                        GTFSTransportationType.tram, TransportMode.Tram, IdSet.emptySet(),
                        Collections.emptySet(), Collections.emptyList(), TramDuration.ofMinutes(13),
                        Collections.emptyList()));
            }
        };

        maxInitialWait = config.getInitialMaxWaitFor(DataSourceID.tfgm);

        txn = createMock(MutableGraphTransaction.class);

        destinationNodeId = TestNodeId.TestOnly(88L);
        startNodeId = TestNodeId.TestOnly(128L);

        long maxNumberOfJourneys = 2;
        queryTime = TramTime.of(8, 15);
        JourneyRequest journeyRequest = new JourneyRequest(
                UpcomingDates.nextSaturday(), queryTime, false,
                config.getMaxNumberChanges(), TramDuration.ofMinutes(config.getMaxJourneyDuration()), maxNumberOfJourneys, TransportMode.TramsOnly);
        reasons = new ServiceReasons(journeyRequest, queryTime, providesNow, failedJourneyDiagnostics);

        serviceHeuristics = createMock(ServiceHeuristics.class);
        //EasyMock.expect(serviceHeuristics.getActualQueryTime()).andStubReturn(queryTime);

        path = createMock(GraphPath.class);
        node = createMock(GraphNode.class);

        final GraphNodeId nodeId = TestNodeId.TestOnly(42L);
        final GraphNodeId previousNodeId = TestNodeId.TestOnly(21L);

        LocationId<Station> approxPosition = Shudehill.getLocationId();
        howIGotHere = new HowIGotHere(nodeId, previousNodeId, TraversalStateType.MinuteState, approxPosition,
                TramStations.MarketStreet.getId(), Collections.emptyList());

        EasyMock.expect(node.getId()).andStubReturn(TestNodeId.TestOnly(42L));

        EasyMock.expect(node.getAllProperties()).andStubReturn(new HashMap<>());

        EasyMock.expect(path.getEndNode(txn)).andReturn(node);

        EasyMock.expect(path.getPreviousNodeId(txn)).andReturn(previousNodeId);

        EasyMock.expect(serviceHeuristics.isDiagnostics()).andStubReturn(false);

    }

    @NotNull
    private NotStartedState getNotStartedState(final GraphNodeId startNodeId) {

        TramDate queryDate = TestEnv.testDay();
        TowardsDestination towardsDestination =  new TowardsDestination(destinationStations);
        StateBuilderParameters builderParams = new StateBuilderParameters(queryDate, queryTime,
                towardsDestination, config, TransportMode.TramsOnly);

        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParams);

        return new NotStartedState(traversalStateFactory, startNodeId, txn);
    }

    @NotNull
    private TramRouteEvaluator getEvaluatorForTest(GraphNodeId destinationNodeId, final boolean isRunning) {
        Set<GraphNodeId> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(destinationNodeId);

        // todo into mock
        Running running = () -> isRunning;
        final ImmutableEnumSet<TransportMode> destinationModes = TransportMode.TramsOnly;
        return new TramRouteEvaluator(serviceHeuristics, config, txn, destinationNodeIds, reasons, previousSuccessfulVisit,
                arrivalHandler, startNodeId, TransportMode.TramsOnly, running, destinationModes, maxInitialWait) {
            @Override
            public GraphEvaluationAction evaluate(GraphPath graphPath, ImmutableJourneyState journeyState) {
                return super.evaluate(graphPath, journeyState);
            }
        };
    }

    @Test
    void shouldHaveReasonsThatInclude() {
        assertEquals(INCLUDE_AND_PRUNE, Arrived.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, ServiceDateOk.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, ServiceTimeOk.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, NumChangesOK.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, TimeOk.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, HourOk.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, Reachable.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, ReachableNoCheck.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, DurationOk.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, WalkOk.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, Continue.getEvaluationAction());
        assertEquals(INCLUDE_AND_CONTINUE, StationOpen.getEvaluationAction());
    }

    @Test
    void shouldHaveReasonsThatExclude() {
        assertEquals(EXCLUDE_AND_PRUNE, HigherCost.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, ReturnedToStart.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, PathTooLong.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, TooManyChanges.getEvaluationAction());
//        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, NotReachable.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, NotOnQueryDate.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, TookTooLong.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, ServiceNotRunningAtTime.getEvaluationAction());

        assertEquals(EXCLUDE_AND_PRUNE, NotAtHour.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, AlreadyDeparted.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, DoesNotOperateOnTime.getEvaluationAction());
        assertEquals(EXCLUDE_AND_PRUNE, StationClosed.getEvaluationAction());

    }

    @Test
    void shouldMatchDestinationLowerCost() {
        GraphNodeId destinationNodeId = TestNodeId.TestOnly(42L);

        JourneyState journeyState = createMock(JourneyState.class);
        TramDuration duration = TramDuration.ofMinutes(42);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(7);

        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(ExchangeSquare.getLocationId());

        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(arrivalHandler.checkDuration(journeyState)).andReturn(ArrivalHandler.Outcome.Better);
        arrivalHandler.recordArrival(journeyState);
        EasyMock.expectLastCall();

        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);

        arrivalHandler.setLowestCost(journeyState);
        EasyMock.expectLastCall();

        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicReasonsOK.Arrived(howIGotHere, duration, 7), node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldStopIfNotRunning() {

        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        JourneyState journeyState = createMock(JourneyState.class);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);
        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(ExchangeSquare.getLocationId());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, false);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldMatchDestinationButHigherCost() {
        GraphNodeId destinationNodeId = TestNodeId.TestOnly(42L);
        TramDuration duration = TramDuration.ofMinutes(100);

        JourneyState journeyState = createMock(JourneyState.class);

        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(10);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);
        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(ExchangeSquare.getLocationId());

        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(arrivalHandler.checkDuration(journeyState)).andReturn(ArrivalHandler.Outcome.Worse);
        arrivalHandler.recordArrival(journeyState);
        EasyMock.expectLastCall();

        previousSuccessfulVisit.cacheVisitIfUseful(
                HeuristicsReasons.ArrivedLater(howIGotHere, duration, 10), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldMatchDestinationButHigherCostButLessHops() {
        GraphNodeId destinationNodeId = TestNodeId.TestOnly(42L);

        JourneyState journeyState = createMock(JourneyState.class);
        TramDuration duration = TramDuration.ofMinutes(100);

        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(2);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);

        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(ExchangeSquare.getLocationId());

        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(arrivalHandler.checkDuration(journeyState)).andReturn(ArrivalHandler.Outcome.Worse);
        arrivalHandler.recordArrival(journeyState);
        EasyMock.expectLastCall();

        //EasyMock.expect(serviceHeuristics.getActualQueryTime()).andStubReturn(queryTime);

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.ArrivedLater(howIGotHere, duration, 2), node, journeyState, labels);

        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldUseCachedResultForMultipleJourneyExclude() {
        GraphNodeId destinationNodeId = TestNodeId.TestOnly(42L);
        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNodeId);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        HeuristicsReason cacheHit = HeuristicsReasons.DoesNotOperateAtHour(time, howIGotHere, 8);
        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(cacheHit);
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfPreviousVisit() {
        GraphNodeId destinationNodeId = TestNodeId.TestOnly(42L);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNodeId);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        final ImmutableEnumSet<GraphLabel> labels = ROUTE_STATION.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        HeuristicsReason cacheHit = HeuristicsReasons.DoesNotOperateOnTime(time, howIGotHere);
        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(cacheHit);

        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldPruneIfPathTooLong() {
        TramDuration duration = TramDuration.ZERO;
        TramTime time = TramTime.of(8, 15);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(200);

        NotStartedState traversalState = getNotStartedState(startNodeId);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        expectContinueForArrivalHandler(journeyState);

        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(path.length()).andReturn(201);

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.PathToLong(howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotRunningToday() {

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));

        NotStartedState traversalState = getNotStartedState(startNodeId);
        TramTime time = TramTime.of(8, 15);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        TramDuration duration = TramDuration.ZERO;
        expectContinueForArrivalHandler(journeyState);

        EasyMock.expect(path.length()).andStubReturn(50);

        final ImmutableEnumSet<GraphLabel> labels = SERVICE.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);


        IdFor<Service> serviceId = Service.createId("nodeServiceId");
        EasyMock.expect(serviceHeuristics.checkServiceDateAndTime(node, howIGotHere, reasons, time, 13)).
                andReturn(HeuristicsReasons.DoesNotRunOnQueryDate(howIGotHere, serviceId));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.DoesNotRunOnQueryDate(howIGotHere, serviceId), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfStationIsClosed() throws TramchesterException {

        final ImmutableEnumSet<GraphLabel> labels = ImmutableEnumSet.of(ROUTE_STATION, TRAM);
        ImmutableEnumSet<GraphLabel> requestedLabels = TRAM.singleton();

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));
        EasyMock.expect(serviceHeuristics.checkModes(labels, requestedLabels, howIGotHere, reasons)).
                andStubReturn(createValidReason(TransportModeOk));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNodeId);
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram, node, true);

        TramDuration duration = TramDuration.ZERO;
        expectContinueForArrivalHandler(journeyState);

        EasyMock.expect(path.length()).andStubReturn(50);

        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(HeuristicsReasons.StationClosed(howIGotHere, Shudehill.getId()));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.StationClosed(howIGotHere, Shudehill.getId()), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        verifyAll();

        assertEquals(EXCLUDE_AND_PRUNE, result);

    }

    private void expectContinueForArrivalHandler(final ImmutableJourneyState journeyState) {
        EasyMock.expect(arrivalHandler.alreadyLonger(journeyState)).andReturn(false);
        EasyMock.expect(arrivalHandler.alreadyMoreChanges(journeyState, 0)).andReturn(false);
        EasyMock.expect(arrivalHandler.overArrivalsLimit(journeyState)).andReturn(false);
    }

    @Test
    void shouldIncludeIfNotOnTramNode() throws TramchesterException {
//        BranchState<JourneyState> branchState = new TestBranchState();

        final ImmutableEnumSet<GraphLabel> labels = ImmutableEnumSet.of(ROUTE_STATION, TRAM);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkModes(labels, TRAM.singleton(), howIGotHere, reasons)).
                andStubReturn(createValidReason(TransportModeOk));
        EasyMock.expect(serviceHeuristics.checkModesMatchForFinalChange(0, ImmutableEnumSet.of(ROUTE_STATION, TRAM),
                TRAM.singleton(), howIGotHere, reasons)).andStubReturn(createValidReason(NumChangesOK));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNodeId);
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Bus, node, true);
//        branchState.setState(journeyState);

        TramDuration duration = TramDuration.ZERO;
        EasyMock.expect(arrivalHandler.alreadyLonger(journeyState)).andStubReturn(false);
        EasyMock.expect(arrivalHandler.alreadyMoreChanges(journeyState,0 )).andReturn(false);
        EasyMock.expect(arrivalHandler.overArrivalsLimit(journeyState)).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, 0, howIGotHere, reasons, time)).
                andReturn(createValidReason(Reachable));
        EasyMock.expect(serviceHeuristics.lowerCostIncludingInterchange(node, howIGotHere, reasons)).andReturn(
                createValidReason(Reachable));

        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(createValidReason(StationOpen));
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(createValidReason(Continue), node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfWalking() {
//        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNodeId);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        TramDuration duration = TramDuration.ZERO;
        expectContinueForArrivalHandler(journeyState);

        EasyMock.expect(path.length()).andStubReturn(50);

        final ImmutableEnumSet<GraphLabel> labels = QUERY_NODE.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);


        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(createValidReason(Continue), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfTakingTooLong() {
//        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));

        TramDuration duration = TramDuration.ZERO;

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNodeId);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        expectContinueForArrivalHandler(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO,howIGotHere, reasons)).
                andReturn(HeuristicsReasons.TookTooLong(time, howIGotHere));

        final ImmutableEnumSet<GraphLabel> labels = ROUTE_STATION.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);


        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.TookTooLong(time, howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfAlreadyTooLong() {
//        BranchState<JourneyState> branchState = new TestBranchState();

        final JourneyState journeyState = createMock(JourneyState.class);
        TramDuration duration = TramDuration.ofMinutes(100);

        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(10);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);

        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(ExchangeSquare.getLocationId());

//        branchState.setState(journeyState);
        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(arrivalHandler.alreadyLonger(journeyState)).andReturn(true);

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.HigherCost(howIGotHere, duration), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfOverLimitOnChanges() {

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(HeuristicsReasons.TooManyChanges(howIGotHere, 5));

        NotStartedState traversalState = getNotStartedState(startNodeId);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        final ImmutableEnumSet<GraphLabel> labels = ROUTE_STATION.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(arrivalHandler.alreadyLonger(journeyState)).andReturn(false);
        EasyMock.expect(arrivalHandler.overArrivalsLimit(journeyState)).andReturn(false);
        EasyMock.expect(arrivalHandler.alreadyMoreChanges(journeyState, 0)).andReturn(false);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.TooManyChanges(howIGotHere, 5), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectHour() {
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));

        NotStartedState traversalState = getNotStartedState(startNodeId);
        TramTime time = TramTime.of(8, 15);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        expectContinueForArrivalHandler(journeyState);

        EasyMock.expect(path.length()).andStubReturn(50);

        final ImmutableEnumSet<GraphLabel> labels = HOUR.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));
        int maxInitialWaitMins = (int) maxInitialWait.toMinutes();
        EasyMock.expect(serviceHeuristics.interestedInHour(howIGotHere, time, reasons, maxInitialWaitMins, HOUR.singleton())).
                andReturn(HeuristicsReasons.DoesNotOperateAtHour(time, howIGotHere, 8));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.DoesNotOperateAtHour(time, howIGotHere, 8), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectMinute() throws TramchesterException {
//        BranchState<JourneyState> branchState = new TestBranchState();

        NotStartedState traversalState = getNotStartedState(startNodeId);
        TramTime time = TramTime.of(8, 15);
        JourneyState journeyState = new JourneyState(time, traversalState);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, node, journeyState, reasons)).
                andStubReturn(createValidReason(Continue));

        expectContinueForArrivalHandler(journeyState);


        EasyMock.expect(path.length()).andStubReturn(50);

        final ImmutableEnumSet<GraphLabel> labels = MINUTE.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        journeyState.board(TransportMode.Tram, node, true); // So uses non-initial wait time
//        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO ,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));
        EasyMock.expect(serviceHeuristics.checkTime(howIGotHere, node, time, reasons, config.getMaxWait())).
                andReturn(HeuristicsReasons.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.DoesNotOperateOnTime(time, howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfSeenSameTripBefore() throws TramchesterException {
//        BranchState<JourneyState> branchState = new TestBranchState();

        IdFor<Trip> tripId = MutableTrip.createId("tripId1");

        NotStartedState traversalState = getNotStartedState(startNodeId);
        TramTime time = TramTime.of(8, 15);
        JourneyState journeyState = new JourneyState(time, traversalState);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));

        EasyMock.expect(serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, node, journeyState, reasons)).
                andStubReturn(createValidReason(Continue));

        expectContinueForArrivalHandler(journeyState);

        EasyMock.expect(path.length()).andStubReturn(50);

        final ImmutableEnumSet<GraphLabel> labels = MINUTE.singleton();
//        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);
        EasyMock.expect(node.getLabels()).andReturn(labels);

        journeyState.board(TransportMode.Tram, node, true); // So uses non-initial wait time
//        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO ,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));

        EasyMock.expect(serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, node, journeyState, reasons)).
                andReturn(HeuristicsReasons.SameTrip(tripId, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.SameTrip(tripId, howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfMatchesNoRules() {
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNodeId);
        final JourneyState journeyState = new JourneyState(time, traversalState);

        expectContinueForArrivalHandler(journeyState);

        EasyMock.expect(path.length()).andStubReturn(50);

        final ImmutableEnumSet<GraphLabel> labels = GROUPED.singleton();
        EasyMock.expect(node.getLabels()).andReturn(labels);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(TramDuration.ZERO,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(createValidReason(Continue), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        GraphEvaluationAction result = evaluator.evaluate(path, journeyState);
        assertEquals(INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @NotNull
    private HeuristicsReason createValidReason(ReasonCode reasonCode) {
        return HeuristicReasonsOK.IsValid(reasonCode, howIGotHere);
    }

    private record TestNodeId(long id) implements GraphNodeId {

        public static GraphNodeId TestOnly(long id) {
                return new TestNodeId(id);
            }
        }



}
