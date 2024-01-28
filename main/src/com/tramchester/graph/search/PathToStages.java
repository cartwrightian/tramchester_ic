package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.graph.facade.GraphTransaction;

import java.util.List;

@ImplementedBy(MapPathToStagesViaStates.class)
public interface PathToStages {
    List<TransportStage<?, ?>> mapDirect(RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest,
                                         LocationSet endStations, GraphTransaction txn, boolean fullLogging);
}
