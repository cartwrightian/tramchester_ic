package com.tramchester.graph.search.inMemory;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.core.*;
import com.tramchester.graph.search.PathRequest;
import com.tramchester.graph.search.PreviousVisits;
import com.tramchester.graph.search.TramNetworkTraverser;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;

import java.util.Set;
import java.util.stream.Stream;

public class TramNetworkTraverserInMemory implements TramNetworkTraverser {
    private final TramchesterConfig config;
    private final Set<GraphNodeId> destinationNodeIds;
    private final GraphTransaction txn;
    private final LocationCollection destinations;

    public TramNetworkTraverserInMemory(TramchesterConfig config, Set<GraphNodeId> destinationNodeIds,
                                        GraphTransaction txn, LocationCollection destinations) {
        this.config = config;
        this.destinationNodeIds = destinationNodeIds;
        this.txn = txn;
        this.destinations = destinations;
    }

    @Override
    public Stream<GraphPath> findPaths(PathRequest pathRequest, PreviousVisits previousVisits, ServiceReasons reasons,
                                       LowestCostSeen lowestCostSeen, TowardsDestination towardsDestination, Running running) {

        final StateBuilderParameters builderParameters = new StateBuilderParameters(pathRequest.getQueryDate(), pathRequest.getActualQueryTime(),
                towardsDestination, config, pathRequest.getRequestedModes());

        final TraversalStateFactory traversalStateFactory = new TraversalStateFactory(builderParameters);

        final GraphNode startNode = pathRequest.getStartNode();
        final GraphNodeId startNodeId = startNode.getId();

        final TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluatorInMemory(pathRequest,
                destinationNodeIds, reasons, previousVisits, lowestCostSeen, config,
                startNodeId, txn, running);


        final FindPathsForJourney searchAlgo = new FindPathsForJourney(txn, startNode, config, tramRouteEvaluator, traversalStateFactory);

        final TramTime actualQueryTime = pathRequest.getActualQueryTime();

        Stream<GraphPath> results = searchAlgo.findPaths(actualQueryTime).stream();

        reasons.reportReasons(txn, pathRequest, destinations);

        return results;
    }
}
