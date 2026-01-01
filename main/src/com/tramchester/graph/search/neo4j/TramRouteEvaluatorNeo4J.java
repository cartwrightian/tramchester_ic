package com.tramchester.graph.search.neo4j;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphPath;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.TramRouteEvaluator;
import com.tramchester.graph.core.neo4j.GraphPathNeo4j;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.diagnostics.GraphEvaluationAction;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;

import java.util.EnumSet;
import java.util.Set;

public class TramRouteEvaluatorNeo4J extends TramRouteEvaluator implements PathEvaluator<JourneyState> {

    public TramRouteEvaluatorNeo4J(final PathRequest pathRequest, final Set<GraphNodeId> destinationNodeIds,
                                   final ServiceReasons reasons,
                                   final PreviousVisits previousVisits, final LowestCostSeen bestResultSoFar, final TramchesterConfig config,
                                   final GraphNodeId startNodeId,
                                   final GraphTransaction txn, Running running) {
        this(pathRequest.getServiceHeuristics(), destinationNodeIds, reasons, previousVisits,
                bestResultSoFar, config, startNodeId, pathRequest.getRequestedModes(),
                pathRequest.getDesintationModes(),
                pathRequest.getMaxInitialWait(), txn, running);
    }

    public TramRouteEvaluatorNeo4J(final ServiceHeuristics serviceHeuristics, final Set<GraphNodeId> destinationNodeIds,
                                   final ServiceReasons reasons,
                                   final PreviousVisits previousVisits, final LowestCostSeen bestResultSoFar, final TramchesterConfig config,
                                   final GraphNodeId startNodeId, final EnumSet<TransportMode> requestedModes,
                                   final EnumSet<TransportMode> destinationModes,
                                   final TramDuration maxInitialWait, final GraphTransaction txn, Running running) {
        super(serviceHeuristics, config, txn, destinationNodeIds, reasons, previousVisits, bestResultSoFar, startNodeId, requestedModes,
                running, destinationModes, maxInitialWait);

    }

    private Evaluation mapEvaluation(final GraphEvaluationAction evaluationAction) {
        return switch (evaluationAction) {
            case EXCLUDE_AND_PRUNE -> Evaluation.EXCLUDE_AND_PRUNE;
            case INCLUDE_AND_PRUNE -> Evaluation.INCLUDE_AND_PRUNE;
            case INCLUDE_AND_CONTINUE -> Evaluation.INCLUDE_AND_CONTINUE;
        };
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(final Path path, final BranchState<JourneyState> state) {
        final ImmutableJourneyState journeyState = state.getState();
        final GraphPath graphPath = GraphPathNeo4j.from(path);

        return mapEvaluation(evaluate(graphPath, journeyState));
    }

}
