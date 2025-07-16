package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.search.stateMachine.TowardsDestination;

import java.util.List;

// TODO remove, only one implementation now
@ImplementedBy(MapPathToStagesViaStates.class)
public interface PathToStages {
    List<TransportStage<?, ?>> mapDirect(RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest,
                                         TowardsDestination towardsDestination, ImmutableGraphTransactionNeo4J txn, boolean fullLogging);
}
