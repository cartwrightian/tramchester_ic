package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
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

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements PathExpander<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final TramchesterConfig config;
    private final GraphTransactionNeo4J txn;
    private final boolean fullLogging;

    public TramNetworkTraverser(ImmutableGraphTransactionNeo4J txn, TramchesterConfig config, boolean fullLogging) {
        this.txn = txn;
        this.fullLogging = fullLogging;
        this.config = config;
    }

    public Stream<GraphPath> findPaths(final ImmutableGraphTransactionNeo4J txn, final RouteCalculatorSupport.PathRequest pathRequest,
                                  final PreviousVisits previousVisits, final ServiceReasons reasons, final LowestCostSeen lowestCostSeen,
                                  final Set<GraphNodeId> destinationNodeIds, final LocationCollection destinations,
                                  final TowardsDestination towardsDestination, final Running running) {

        final StateBuilderParameters builderParameters = new StateBuilderParameters(pathRequest.getQueryDate(), pathRequest.getActualQueryTime(),
                towardsDestination, config, pathRequest.getRequestedModes());

        final TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParameters);

        final BranchOrderingPolicy selector = pathRequest.getSelector();
        final TramTime actualQueryTime = pathRequest.getActualQueryTime();

        final GraphNode startNode = pathRequest.getStartNode();
        final GraphNodeId startNodeId = startNode.getId();

        final TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluator(pathRequest,
                destinationNodeIds, reasons, previousVisits, lowestCostSeen, config,
                startNodeId, txn, running);

        final NotStartedState traversalState = new NotStartedState(traversalStateFactory, startNodeId, txn);
        final InitialBranchState<JourneyState> initialJourneyState = JourneyState.initialState(actualQueryTime, traversalState);

        if (fullLogging) {
            logger.info("Create traversal for " + actualQueryTime);
        }

        final TraversalDescription traversalDesc =
                new MonoDirectionalTraversalDescription().
                        // api updated, the call to expand overrides any calls to relationships
                uniqueness(NONE).
                expand(this, initialJourneyState).
                order(selector).
                evaluator(tramRouteEvaluator);

        final Traverser traverse = startNode.getTraverserFor(traversalDesc);
        final Spliterator<Path> spliterator = traverse.spliterator();

        final Stream<Path> stream = StreamSupport.stream(spliterator, false);

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() -> {
            if (fullLogging) {
                reasons.reportReasons(txn, pathRequest, destinations);
                previousVisits.reportStats();
            }
            //traversalState.dispose();
        });

        if (fullLogging) {
            logger.info("Return traversal stream");
        }
        return stream.filter(path -> {
            final GraphNodeId endPathNodeId = txn.endNodeNodeId(path);

            return destinationNodeIds.contains(endPathNodeId);
            }).map(GraphPathNeo4j::from);
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
        final Stream<ImmutableGraphRelationship> outbounds = traversalStateForChildren.getOutbounds();
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
