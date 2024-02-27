package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.caches.NodeContentsRepository;

public record StateBuilderParameters(TramDate queryDate, int queryHour,
                                     LocationCollection destinationIds,
                                     NodeContentsRepository nodeContents,
                                     boolean depthFirst,
                                     boolean interchangesOnly) {
}
