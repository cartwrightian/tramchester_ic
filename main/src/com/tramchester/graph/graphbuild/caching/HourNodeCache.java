package com.tramchester.graph.graphbuild.caching;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.neo4j.MutableGraphNodeNeo4J;

public interface HourNodeCache {

    void putHour(GraphNodeId id, int hour, GraphNode hourNode);

    boolean hasHourNode(GraphNodeId id, int hour);

    MutableGraphNodeNeo4J getHourNode(MutableGraphTransaction tx, GraphNodeId id, int hour);
}
