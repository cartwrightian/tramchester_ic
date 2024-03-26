package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Service;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.ServiceHeuristics;
import com.tramchester.graph.search.TramRouteEvaluator;
import com.tramchester.graph.search.diagnostics.*;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static com.tramchester.graph.search.diagnostics.ReasonCode.*;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramRouteEvaluatorTest extends EasyMockSupport {

    private ServiceHeuristics serviceHeuristics;
    private NodeContentsRepository contentsRepository;
    private Path path;
    private HowIGotHere howIGotHere;
    private ImmutableGraphNode node;
    private ServiceReasons reasons;
    private TramchesterConfig config;
    private PreviousVisits previousSuccessfulVisit;
    private GraphNodeId destinationNodeId;
    private ImmutableGraphRelationship lastRelationship;
    private TripRepository tripRepository;
    private GraphNodeId startNodeId;
    private LowestCostSeen lowestCostSeen;
    private ProvidesNow providesNow;
    private Duration maxInitialWait;
    private MutableGraphTransaction txn;
    private GraphNode startNode;
    private LocationSet<Station> destinationStations;
    private TramTime queryTime;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        MutableStation forTest = new MutableStation(Station.createId("destinationStationId"),
                NPTGLocality.createId("area"), "name",
                new LatLong(1, 1), new GridPosition(1000,1000), DataSourceID.tfgm,
                false);

        destinationStations = LocationSet.singleton(forTest);

        forTest.addRouteDropOff(TestEnv.getTramTestRoute());
        forTest.addRouteDropOff(TestEnv.getTramTestRoute());
        forTest.addRoutePickUp(TestEnv.getTramTestRoute());
        forTest.addRoutePickUp(TestEnv.getTramTestRoute());

        lowestCostSeen = createMock(LowestCostSeen.class);
        previousSuccessfulVisit = createMock(PreviousVisits.class);
        tripRepository = createMock(TripRepository.class);
        CreateJourneyDiagnostics failedJourneyDiagnostics = createMock(CreateJourneyDiagnostics.class);

        contentsRepository = createMock(NodeContentsRepository.class);

        providesNow = createMock(ProvidesNow.class);


        config = new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(new TFGMGTFSSourceTestConfig(
                        GTFSTransportationType.tram, TransportMode.Tram, IdSet.emptySet(),
                        Collections.emptySet(), Collections.emptyList(), Duration.ofMinutes(13)));
            }
        };

        maxInitialWait = config.getInitialMaxWaitFor(DataSourceID.tfgm);

        txn = createMock(MutableGraphTransaction.class);

        destinationNodeId = GraphNodeId.TestOnly(88L);
        startNodeId = GraphNodeId.TestOnly(128L);
        startNode = createMock(GraphNode.class);

        long maxNumberOfJourneys = 2;
        queryTime = TramTime.of(8, 15);
        JourneyRequest journeyRequest = new JourneyRequest(
                TestEnv.nextSaturday(), queryTime, false,
                3, Duration.ofMinutes(config.getMaxJourneyDuration()), maxNumberOfJourneys, TramsOnly);
        reasons = new ServiceReasons(journeyRequest, queryTime, providesNow, failedJourneyDiagnostics);

        serviceHeuristics = createMock(ServiceHeuristics.class);
        path = createMock(Path.class);
        node = createMock(ImmutableGraphNode.class);
        lastRelationship = createMock(ImmutableGraphRelationship.class);

        final GraphNodeId nodeId = GraphNodeId.TestOnly(42L);
        final GraphNodeId previousNodeId = GraphNodeId.TestOnly(21L);

        IdFor<Station> approxPosition = Shudehill.getId();
        howIGotHere = new HowIGotHere(nodeId, previousNodeId, TraversalStateType.MinuteState, approxPosition,
                TramStations.MarketStreet.getId());

        EasyMock.expect(node.getId()).andStubReturn(GraphNodeId.TestOnly(42L));
        EasyMock.expect(node.getAllProperties()).andStubReturn(new HashMap<>());

        EasyMock.expect(txn.fromEnd(path)).andReturn(node);
        EasyMock.expect(txn.lastFrom(path)).andStubReturn(lastRelationship);

        EasyMock.expect(lastRelationship.getStartNodeId(txn)).andStubReturn(previousNodeId);

    }

    @NotNull
    private NotStartedState getNotStartedState(GraphNode startNode) {

        TramDate queryDate = TestEnv.testDay();
        TowardsDestination towardsDestination =  new TowardsDestination(destinationStations);
        StateBuilderParameters builderParams = new StateBuilderParameters(queryDate, queryTime, destinationStations,
                towardsDestination, contentsRepository, config, TramsOnly);

        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParams);

        final TraversalOps traversalOps = new TraversalOps(txn, contentsRepository, tripRepository);
        return new NotStartedState(traversalOps, traversalStateFactory, startNode, txn);
    }

    @NotNull
    private TramRouteEvaluator getEvaluatorForTest(GraphNodeId destinationNodeId, final boolean isRunning) {
        Set<GraphNodeId> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(destinationNodeId);

        // todo into mock
        Running running = () -> isRunning;
        return new TramRouteEvaluator(serviceHeuristics, destinationNodeIds, contentsRepository,
                reasons, previousSuccessfulVisit, lowestCostSeen, config, startNodeId, TramsOnly, maxInitialWait, txn, running);
    }

    @Test
    void shouldHaveReasonsThatInclude() {
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, Arrived.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, ServiceDateOk.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, ServiceTimeOk.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, NumChangesOK.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TimeOk.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, HourOk.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, Reachable.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, ReachableNoCheck.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, DurationOk.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, WalkOk.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, Continue.getEvaluationAction());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, StationOpen.getEvaluationAction());

    }

    @Test
    void shouldHaveReasonsThatExclude() {
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, HigherCost.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, ReturnedToStart.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, PathTooLong.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TooManyChanges.getEvaluationAction());
//        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, NotReachable.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, NotOnQueryDate.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TookTooLong.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, ServiceNotRunningAtTime.getEvaluationAction());

        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, NotAtHour.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, AlreadyDeparted.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, DoesNotOperateOnTime.getEvaluationAction());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, StationClosed.getEvaluationAction());

    }

    @Test
    void shouldMatchDestinationLowerCost() {
        GraphNodeId destinationNodeId = GraphNodeId.TestOnly(42L);

        BranchState<JourneyState> branchState = new TestBranchState();

        JourneyState journeyState = createMock(JourneyState.class);
        Duration duration = Duration.ofMinutes(42);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(7);

        //EasyMock.expect(journeyState.approxPosition()).andStubReturn(TramStations.ExchangeSquare.getId());
        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(TramStations.ExchangeSquare.getId());

        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(lowestCostSeen.isLower(journeyState)).andReturn(true);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);

        lowestCostSeen.setLowestCost(journeyState);
        EasyMock.expectLastCall();

        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicReasonsOK.Arrived(howIGotHere, duration, 7), node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldStopIfNotRunning() {

        resetAll();
        BranchState<JourneyState> branchState = new TestBranchState();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, false);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldMatchDestinationButHigherCost() {
        GraphNodeId destinationNodeId = GraphNodeId.TestOnly(42L);

        BranchState<JourneyState> branchState = new TestBranchState();

        JourneyState journeyState = createMock(JourneyState.class);
        Duration duration = Duration.ofMinutes(100);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(10);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);

        //EasyMock.expect(journeyState.approxPosition()).andStubReturn(TramStations.ExchangeSquare.getId());
        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(TramStations.ExchangeSquare.getId());

        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(lowestCostSeen.isLower(journeyState)).andReturn(false);
        EasyMock.expect(lowestCostSeen.getLowestNumChanges()).andReturn(5);

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.ArrivedMoreChanges(howIGotHere, 10, duration), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldMatchDestinationButHigherCostButLessHops() {
        GraphNodeId destinationNodeId = GraphNodeId.TestOnly(42L);

        BranchState<JourneyState> branchState = new TestBranchState();

        JourneyState journeyState = createMock(JourneyState.class);
        Duration duration = Duration.ofMinutes(100);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(2);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);
        //EasyMock.expect(journeyState.approxPosition()).andStubReturn(TramStations.ExchangeSquare.getId());
        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(TramStations.ExchangeSquare.getId());

        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        EasyMock.expect(lowestCostSeen.getLowestNumChanges()).andReturn(5);
        EasyMock.expect(lowestCostSeen.isLower(journeyState)).andReturn(false);

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicReasonsOK.Arrived(howIGotHere, duration, 2), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldUseCachedResultForMultipleJourneyExclude() {
        GraphNodeId destinationNodeId = GraphNodeId.TestOnly(42L);

        BranchState<JourneyState> state = new TestBranchState();

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        state.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        HeuristicsReason cacheHit = HeuristicsReasons.DoesNotOperateAtHour(time, howIGotHere, 8);
        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(cacheHit);
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfPreviousVisit() {
        GraphNodeId destinationNodeId = GraphNodeId.TestOnly(42L);

        BranchState<JourneyState> state = new TestBranchState();

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        state.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        HeuristicsReason cacheHit = HeuristicsReasons.DoesNotOperateOnTime(time, howIGotHere);
        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(cacheHit);

        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldPruneIfTooLong() {
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(200);

        BranchState<JourneyState> branchState = new TestBranchState();
        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);
        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(path.length()).andReturn(201);

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.PathToLong(howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
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
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);
        BranchState<JourneyState> branchState = new TestBranchState();

        final EnumSet<GraphLabel> labels = EnumSet.of(SERVICE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        TramTime time = TramTime.of(8, 15);

        IdFor<Service> serviceId = Service.createId("nodeServiceId");
        EasyMock.expect(serviceHeuristics.checkServiceDateAndTime(node, howIGotHere, reasons, time, 13)).
                andReturn(HeuristicsReasons.DoesNotRunOnQueryDate(howIGotHere, serviceId));

        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.DoesNotRunOnQueryDate(howIGotHere, serviceId), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

//    @Test
//    void shouldExcludeIfUnreachableNode() throws TramchesterException {
//        BranchState<JourneyState> branchState = new TestBranchState();
//
//        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION, TRAM);
//
//        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
//        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
//                andStubReturn(HeuristicsReasons.IsValid(ReasonCode.NumChangesOK, howIGotHere));
//        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
//                andStubReturn(HeuristicsReasons.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
//        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
//                andStubReturn(HeuristicsReasons.IsValid(DurationOk, howIGotHere));
//        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
//                andStubReturn(HeuristicsReasons.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
//        EasyMock.expect(serviceHeuristics.checkModes(labels, EnumSet.of(TRAM), howIGotHere, reasons)).
//                andStubReturn(HeuristicsReasons.IsValid(ReasonCode.TransportModeOk, howIGotHere));
//        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
//                andStubReturn(HeuristicsReasons.IsValid(StationOpen, howIGotHere));
//
//        TramTime time = TramTime.of(8, 15);
//        NotStartedState traversalState = getNotStartedState(startNode);
//        JourneyState journeyState = new JourneyState(time, traversalState);
//        journeyState.board(TransportMode.Tram, node, true);
//        branchState.setState(journeyState);
//
//        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);
//
//        EasyMock.expect(path.length()).andStubReturn(50);
//        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);
//
//        EasyMock.expect(serviceHeuristics.canReachDestination(node, 0, howIGotHere, reasons, time)).
//                andReturn(HeuristicsReasons.StationNotReachable(howIGotHere, ReasonCode.NotReachable));
//
//        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
//
//        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.NotReachable, node, journeyState, labels);
//        EasyMock.expectLastCall();
//        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());
//
//        replayAll();
//        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
//        Evaluation result = evaluator.evaluate(path, branchState);
//        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
//        verifyAll();
//    }

    @Test
    void shouldExcludeIfStationIsClosed() throws TramchesterException {
        BranchState<JourneyState> branchState = new TestBranchState();

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION, TRAM);
        EnumSet<GraphLabel> requestedLabels = EnumSet.of(TRAM);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));
        EasyMock.expect(serviceHeuristics.checkModes(labels, requestedLabels, howIGotHere, reasons)).
                andStubReturn(createValidReason(TransportModeOk));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNode);
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram, node, true);
        branchState.setState(journeyState);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(HeuristicsReasons.StationClosed(howIGotHere, Shudehill.getId()));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.StationClosed(howIGotHere, Shudehill.getId()), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        verifyAll();

        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);

    }

    @Test
    void shouldIncludeIfNotOnTramNode() throws TramchesterException {
        BranchState<JourneyState> branchState = new TestBranchState();

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION, TRAM);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkModes(labels, EnumSet.of(TRAM), howIGotHere, reasons)).
                andStubReturn(createValidReason(TransportModeOk));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNode);
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Bus, node, true);
        branchState.setState(journeyState);

        EasyMock.expect(lowestCostSeen.everArrived()).andStubReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lastRelationship.isType(WALKS_TO_STATION)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, 0, howIGotHere, reasons, time)).
                andReturn(createValidReason(Reachable));
        EasyMock.expect(serviceHeuristics.lowerCostIncludingInterchange(node, howIGotHere, reasons)).andReturn(
                createValidReason(Reachable));

        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(createValidReason(StationOpen));
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(createValidReason(WalkOk), node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfWalking() {
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(createValidReason(DurationOk));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(QUERY_NODE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lastRelationship.isType(WALKS_TO_STATION)).andReturn(true);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(createValidReason(WalkOk), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfTakingTooLong() {
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO,howIGotHere, reasons)).
                andReturn(HeuristicsReasons.TookTooLong(time, howIGotHere));

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.TookTooLong(time, howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfAlreadyTooLong() {
        BranchState<JourneyState> branchState = new TestBranchState();

        final JourneyState journeyState = createMock(JourneyState.class);
        Duration duration = Duration.ofMinutes(100);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(duration);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(10);
        EasyMock.expect(journeyState.getTraversalStateType()).andStubReturn(TraversalStateType.PlatformState);
        //EasyMock.expect(journeyState.approxPosition()).andStubReturn(TramStations.ExchangeSquare.getId());
        journeyState.approxPosition();
        EasyMock.expectLastCall().andStubReturn(TramStations.ExchangeSquare.getId());

        branchState.setState(journeyState);
        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).
                andReturn(HeuristicsReasons.CacheMiss(howIGotHere));

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(true);
        EasyMock.expect(lowestCostSeen.getLowestDuration()).andReturn(Duration.ofMinutes(10));

        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.HigherCost(howIGotHere, duration), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfOverLimitOnChanges() {
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(HeuristicsReasons.TooManyChanges(howIGotHere, 5));

        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.TooManyChanges(howIGotHere, 5), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectHour() {
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        NotStartedState traversalState = getNotStartedState(startNode);
        TramTime time = TramTime.of(8, 15);

        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));
        int maxInitialWaitMins = (int) maxInitialWait.toMinutes();
        EasyMock.expect(serviceHeuristics.interestedInHour(howIGotHere, time, reasons, maxInitialWaitMins, EnumSet.of(HOUR))).
                andReturn(HeuristicsReasons.DoesNotOperateAtHour(time, howIGotHere, 8));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.DoesNotOperateAtHour(time, howIGotHere, 8), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectMinute() throws TramchesterException {
        BranchState<JourneyState> branchState = new TestBranchState();

        NotStartedState traversalState = getNotStartedState(startNode);
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

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(MINUTE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        journeyState.board(TransportMode.Tram, node, true); // So uses non-initial wait time
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO ,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));
        EasyMock.expect(serviceHeuristics.checkTime(howIGotHere, node, time, reasons, config.getMaxWait())).
                andReturn(HeuristicsReasons.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.DoesNotOperateOnTime(time, howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfSeenSameTripBefore() throws TramchesterException {
        BranchState<JourneyState> branchState = new TestBranchState();

        IdFor<Trip> tripId = MutableTrip.createId("tripId1");

        NotStartedState traversalState = getNotStartedState(startNode);
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

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(MINUTE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        journeyState.board(TransportMode.Tram, node, true); // So uses non-initial wait time
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO ,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));

        EasyMock.expect(serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, node, journeyState, reasons)).
                andReturn(HeuristicsReasons.SameTrip(tripId, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(HeuristicsReasons.SameTrip(tripId, howIGotHere), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfMatchesNoRules() {
        BranchState<JourneyState> branchState = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumChangesOK));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NumConnectionsOk));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(createValidReason(NeighbourConnectionsOk));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);
        EasyMock.expect(lastRelationship.isType(WALKS_TO_STATION)).andReturn(false);

        final EnumSet<GraphLabel> labels = EnumSet.of(GROUPED);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState(startNode);
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO,howIGotHere, reasons)).
                andReturn(createValidReason(DurationOk));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(journeyState, labels, howIGotHere)).andReturn(HeuristicsReasons.CacheMiss(howIGotHere));
        previousSuccessfulVisit.cacheVisitIfUseful(createValidReason(Continue), node, journeyState, labels);
        EasyMock.expectLastCall();
        EasyMock.expect(providesNow.getInstant()).andStubReturn(Instant.now());

        replayAll();
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId, true);
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @NotNull
    private HeuristicsReason createValidReason(ReasonCode reasonCode) {
        return HeuristicReasonsOK.IsValid(reasonCode, howIGotHere);
    }

    private static class TestBranchState implements BranchState<JourneyState> {
        private JourneyState journeyState;

        @Override
        public JourneyState getState() {
            return journeyState;
        }

        @Override
        public void setState(JourneyState journeyState) {
            this.journeyState = journeyState;
        }
    }

}
