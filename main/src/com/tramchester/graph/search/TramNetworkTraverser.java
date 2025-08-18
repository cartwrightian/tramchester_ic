package com.tramchester.graph.search;

import com.tramchester.domain.collections.Running;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.core.GraphPath;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.stateMachine.TowardsDestination;

import java.util.stream.Stream;

public interface TramNetworkTraverser {
    Stream<GraphPath> findPaths(PathRequest pathRequest,
                                PreviousVisits previousVisits, ServiceReasons reasons, LowestCostSeen lowestCostSeen,
                                TowardsDestination towardsDestination,
                                Running running);
}
