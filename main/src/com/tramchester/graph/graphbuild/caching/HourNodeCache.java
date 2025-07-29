package com.tramchester.graph.graphbuild.caching;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.neo4j.MutableGraphNode;

public interface HourNodeCache {

    void putHour(GraphNodeId id, int hour, GraphNode hourNode);

    boolean hasHourNode(GraphNodeId id, int hour);

    MutableGraphNode getHourNode(MutableGraphTransaction tx, GraphNodeId id, int hour);
}
