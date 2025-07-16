package com.tramchester.graph.graphbuild.caching;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.GraphNodeId;
import com.tramchester.graph.facade.neo4j.MutableGraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;

public interface HourNodeCache {

    void putHour(GraphNodeId id, int hour, GraphNode hourNode);

    boolean hasHourNode(GraphNodeId id, int hour);

    MutableGraphNode getHourNode(MutableGraphTransactionNeo4J tx, GraphNodeId id, int hour);
}
