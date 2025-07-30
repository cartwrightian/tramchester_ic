package com.tramchester.graph.graphbuild.caching;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;

public interface HourNodeCache {

    void putHour(GraphNodeId id, int hour, GraphNode hourNode);

    boolean hasHourNode(GraphNodeId id, int hour);

    MutableGraphNode getHourNode(MutableGraphTransaction tx, GraphNodeId id, int hour);
}
