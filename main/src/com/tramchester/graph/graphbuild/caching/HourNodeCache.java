package com.tramchester.graph.graphbuild.caching;

import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.core.MutableGraphTransaction;

public interface HourNodeCache {

    void putHour(GraphNodeId id, int hour, GraphNode hourNode);

    boolean hasHourNode(GraphNodeId id, int hour);

    MutableGraphNode getHourNode(MutableGraphTransaction tx, GraphNodeId id, int hour);
}
