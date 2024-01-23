package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StationDistances;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.ImmuatableTraversalState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;
import static org.neo4j.graphdb.traversal.BranchOrderingPolicies.PREORDER_DEPTH_FIRST;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements PathExpander<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRespository;
    private final TramTime actualQueryTime;
    private final Set<GraphNodeId> destinationNodeIds;
    private final LocationSet destinations;
    private final TramchesterConfig config;
    private final ServiceReasons reasons;
    private final TraversalStateFactory traversalStateFactory;
    private final RouteCalculatorSupport.PathRequest pathRequest;
    private final ReasonsToGraphViz reasonToGraphViz;
    private final ProvidesNow providesNow;
    private final GraphTransaction txn;
    private final StationDistances stationDistances;

    public TramNetworkTraverser(GraphTransaction txn, RouteCalculatorSupport.PathRequest pathRequest,
                                NodeContentsRepository nodeContentsRepository, TripRepository tripRepository,
                                TraversalStateFactory traversalStateFactory, LocationSet destinations, TramchesterConfig config,
                                Set<GraphNodeId> destinationNodeIds, ServiceReasons reasons,
                                ReasonsToGraphViz reasonToGraphViz, ProvidesNow providesNow, StationDistances stationDistances) {
        this.txn = txn;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRespository = tripRepository;
        this.traversalStateFactory = traversalStateFactory;
        this.destinationNodeIds = destinationNodeIds;
        this.destinations = destinations;
        this.config = config;
        this.reasons = reasons;
        this.pathRequest = pathRequest;

        this.actualQueryTime = pathRequest.getActualQueryTime();
        this.reasonToGraphViz = reasonToGraphViz;
        this.providesNow = providesNow;
        this.stationDistances = stationDistances;
    }

    public Stream<Path> findPaths(GraphTransaction txn, GraphNode startNode, PreviousVisits previousSuccessfulVisit, LowestCostSeen lowestCostSeen) {
        final boolean depthFirst = config.getDepthFirst();
        if (depthFirst) {
            logger.info("Depth first is enabled");
        } else {
            logger.info("Breadth first is enabled");
        }

        Instant begin = providesNow.getInstant();
        Duration maxInitialWait = pathRequest.getMaxInitialWait();
        final TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluator(pathRequest.getServiceHeuristics(),
                destinationNodeIds, nodeContentsRepository, reasons, previousSuccessfulVisit, lowestCostSeen, config,
                startNode.getId(), begin, providesNow, pathRequest.getRequestedModes(), maxInitialWait, txn);

        TraversalOps traversalOps = new TraversalOps(txn, nodeContentsRepository, tripRespository, destinations, pathRequest.getQueryDate());

        final NotStartedState traversalState = new NotStartedState(traversalOps, traversalStateFactory,
                pathRequest.getRequestedModes(), startNode);
        final InitialBranchState<JourneyState> initialJourneyState = JourneyState.initialState(actualQueryTime, traversalState);

        logger.info("Create traversal for " + actualQueryTime);

        // note: for small graphs (i.e. trams only) depth first is far faster, for larger (i.e. bus) trying out breadth
        final BranchOrderingPolicy selector = depthFirst ? PREORDER_DEPTH_FIRST
                : (start, expander) -> new SpikeBranchSelector(start, expander, stationDistances, destinations);

        TraversalDescription traversalDesc =
                new MonoDirectionalTraversalDescription().
                        // api updated, the call to expand overrides any calls to relationships
                uniqueness(NONE).
                expand(this, initialJourneyState).
                order(selector).
                evaluator(tramRouteEvaluator);

        Traverser traverse = startNode.getTraverserFor(traversalDesc);
        Spliterator<Path> spliterator = traverse.spliterator();

        Stream<Path> stream = StreamSupport.stream(spliterator, false);

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() -> {
            reasons.reportReasons(txn, pathRequest, reasonToGraphViz);
            previousSuccessfulVisit.reportStats();
            traversalState.dispose();
        });

        logger.info("Return traversal stream");
        return stream.filter(path -> destinationNodeIds.contains(txn.fromEnd(path).getId()));
    }

    @Override
    public ResourceIterable<Relationship> expand(final Path path, final BranchState<JourneyState> graphState) {
        final ImmutableJourneyState currentState = graphState.getState();
        final ImmuatableTraversalState traversalState = currentState.getTraversalState();

        final GraphNode endPathNode =  txn.fromEnd(path);
        final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentState);

        final GraphRelationship lastRelationship = txn.lastFrom(path);
        final Duration cost;
        if (lastRelationship !=null) {
            cost = nodeContentsRepository.getCost(lastRelationship);
            if (Durations.greaterThan(cost, Duration.ZERO)) {
                final Duration totalCost = currentState.getTotalDurationSoFar();
                final Duration total = totalCost.plus(cost);
                journeyStateForChildren.updateTotalCost(total);
            }
            if (lastRelationship.isType(DIVERSION)) {
                final IdFor<Station> stationId = lastRelationship.getStartStationId();
                journeyStateForChildren.beginDiversion(stationId);
            }
        } else {
            cost = Duration.ZERO;
        }

        final EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(endPathNode);

        final TraversalState traversalStateForChildren = traversalState.nextState(labels, endPathNode,
                journeyStateForChildren, cost, journeyStateForChildren.isOnDiversion());

        journeyStateForChildren.updateTraversalState(traversalStateForChildren);
        graphState.setState(journeyStateForChildren);

        Stream<ImmutableGraphRelationship> outbounds = traversalStateForChildren.getOutbounds(txn, pathRequest);
        return convertToIter(outbounds);
    }

    private ResourceIterable<Relationship> convertToIter(final Stream<ImmutableGraphRelationship> resourceIterable) {
        return ImmutableGraphRelationship.convertIterable(resourceIterable);
    }

    @Override
    public PathExpander<JourneyState> reverse() {
        return null;
    }


}
