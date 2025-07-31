package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.neo4j.RouteCalculator;
import com.tramchester.graph.search.stateMachine.TowardsDestination;

import java.util.List;

// TODO remove, only one implementation now
@ImplementedBy(MapPathToStagesViaStates.class)
public interface PathToStages {
    List<TransportStage<?, ?>> mapDirect(RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest,
                                         TowardsDestination towardsDestination, GraphTransaction txn, boolean fullLogging);
}
