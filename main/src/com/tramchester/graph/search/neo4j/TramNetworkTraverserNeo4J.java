package com.tramchester.graph.search.neo4j;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.neo4j.CreateGraphTraverser;
import com.tramchester.graph.core.neo4j.GraphPathNeo4j;
import com.tramchester.graph.core.neo4j.ImmutableGraphRelationshipNeo4J;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.*;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.reference.TransportRelationshipTypes.DIVERSION;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverserNeo4J implements PathExpander<JourneyState>, TramNetworkTraverser {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverserNeo4J.class);

    private final TramchesterConfig config;
    private final BranchOrderingPolicy orderingPolicy;
    private final GraphTransaction txn;
    private final boolean fullLogging;
    private final LocationCollection destinations;

    public TramNetworkTraverserNeo4J(GraphTransaction txn, TramchesterConfig config, boolean fullLogging, BranchOrderingPolicy orderingPolicy,
                                     LocationCollection destinations) {
        this.txn = txn;
        this.fullLogging = fullLogging;
        this.config = config;
        this.orderingPolicy = orderingPolicy;
        this.destinations = destinations;
    }


    public static InitialBranchState<JourneyState> initialState(final TramTime queryTime, final TraversalState traversalState) {
        return new InitialBranchState<>() {
            @Override
            public JourneyState initialState(Path path) {
                return new JourneyState(queryTime, traversalState);
            }

            @Override
            public InitialBranchState<JourneyState> reverse() {
                return null;
            }
        };
    }

    @Override
    public Stream<GraphPath> findPaths(final PathRequest pathRequest,
                                       final PreviousVisits previousVisits, final ServiceReasons reasons, final LowestCostSeen lowestCostSeen,
                                       final TowardsDestination towardsDestination,
                                       final Set<GraphNodeId> destinationNodeIds,
                                       final Running running) {

        final StateBuilderParameters builderParameters = new StateBuilderParameters(pathRequest.getQueryDate(), pathRequest.getActualQueryTime(),
                towardsDestination, config, pathRequest.getRequestedModes());

        final TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParameters);

        final TramTime actualQueryTime = pathRequest.getActualQueryTime();

        final GraphNode startNode = pathRequest.getStartNode();
        final GraphNodeId startNodeId = startNode.getId();

        final TramRouteEvaluatorNeo4J tramRouteEvaluator = new TramRouteEvaluatorNeo4J(pathRequest,
                destinationNodeIds, reasons, previousVisits, lowestCostSeen, config,
                startNodeId, txn, running);

        final NotStartedState traversalState = new NotStartedState(traversalStateFactory, startNodeId, txn);
        final InitialBranchState<JourneyState> initialJourneyState = initialState(actualQueryTime, traversalState);

        if (fullLogging) {
            logger.info("Create traversal for " + actualQueryTime);
        }

        final TraversalDescription traversalDesc =
                new MonoDirectionalTraversalDescription().
                        // api updated, the call to expand overrides any calls to relationships
                uniqueness(NONE).
                expand(this, initialJourneyState).
                order(orderingPolicy).
                evaluator(tramRouteEvaluator);

//        final Traverser traverse = startNode.getTraverserFor(traversalDesc);
        final Traverser traverse = getTraverser(traversalDesc, startNode);
        final Spliterator<Path> spliterator = traverse.spliterator();

        final Stream<Path> stream = StreamSupport.stream(spliterator, false);

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() -> {
            if (fullLogging) {
                reasons.reportReasons(txn, pathRequest.getNumChanges(), destinations);
                previousVisits.reportStats();
            }
            //traversalState.dispose();
        });

        if (fullLogging) {
            logger.info("Return traversal stream");
        }
        return stream.
                map(GraphPathNeo4j::from).
                filter(path -> {
                    final GraphNodeId endPathNodeId = path.getEndNodeId(txn); //txn.endNodeNodeId(path);
                    return destinationNodeIds.contains(endPathNodeId);
            }).map(item -> item);
    }

    private Traverser getTraverser(final TraversalDescription traversalDesc, final GraphNode graphNode) {
        if (graphNode instanceof CreateGraphTraverser createGraphTraverser) {
            return createGraphTraverser.getTraverser(traversalDesc);
        } else {
            throw new RuntimeException("GraphNode is not a CreateGraphTraverser, got a " + graphNode.toString());
        }
    }

    @Override
    public ResourceIterable<Relationship> expand(final Path path, final BranchState<JourneyState> graphState) {
        return expand(GraphPathNeo4j.from(path), graphState);
    }

    public ResourceIterable<Relationship> expand(final GraphPath path, final BranchState<JourneyState> graphState) {
        // GraphState -> JourneyState -> TraversalState
        final ImmutableJourneyState currentJourneyState = graphState.getState();
        final ImmutableTraversalState currentTraversalState = currentJourneyState.getTraversalState();

        final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentJourneyState);

        final GraphRelationship lastRelationship = path.getLastRelationship(txn); //txn.lastFrom(path);

        final Duration cost;
        if (lastRelationship != null) {
            cost = lastRelationship.getCost();

            if (Durations.greaterThan(cost, Duration.ZERO)) {
                final Duration totalCost = currentJourneyState.getTotalDurationSoFar();
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

        final GraphNode endPathNode =  path.getEndNode(txn); // txn.fromEnd(path);

        final EnumSet<GraphLabel> labels = endPathNode.getLabels();

        final ImmutableTraversalState traversalStateForChildren = currentTraversalState.nextState(labels, endPathNode,
                journeyStateForChildren, cost);

        journeyStateForChildren.updateTraversalState(traversalStateForChildren);

        graphState.setState(journeyStateForChildren);
        final Stream<GraphRelationship> outbounds = traversalStateForChildren.getOutbounds();
        return convertToIter(outbounds);
    }

    private ResourceIterable<Relationship> convertToIter(final Stream<GraphRelationship> resourceIterable) {
        return ImmutableGraphRelationshipNeo4J.convertIterable(resourceIterable);
    }

    @Override
    public PathExpander<JourneyState> reverse() {
        return null;
    }

}
