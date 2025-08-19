package com.tramchester.graph.search.inMemory;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.Running;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.TramRouteEvaluator;
import com.tramchester.graph.search.PathRequest;
import com.tramchester.graph.search.PreviousVisits;
import com.tramchester.graph.search.diagnostics.ServiceReasons;

import java.util.Set;

public class TramRouteEvaluatorInMemory extends TramRouteEvaluator {
    public TramRouteEvaluatorInMemory(final PathRequest pathRequest, final Set<GraphNodeId> destinationNodeIds,
                                      final ServiceReasons reasons,
                                      final PreviousVisits previousVisits, final LowestCostSeen bestResultSoFar, final TramchesterConfig config,
                                      final GraphNodeId startNodeId,
                                      final GraphTransaction txn, Running running) {
        super(pathRequest.getServiceHeuristics(), config, txn, destinationNodeIds, reasons, previousVisits, bestResultSoFar, startNodeId,
                pathRequest.getRequestedModes(), running, pathRequest.getDesintationModes(), pathRequest.getMaxInitialWait());
    }
}
