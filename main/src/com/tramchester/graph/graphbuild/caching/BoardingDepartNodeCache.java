package com.tramchester.graph.graphbuild.caching;

import com.tramchester.graph.facade.GraphNodeId;

public interface BoardingDepartNodeCache {

    boolean hasBoarding(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId);
    boolean hasDeparts(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId);

    void putDepart(GraphNodeId boardingNodeId, GraphNodeId routeStationNodeId);
    void putBoarding(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId);

}
