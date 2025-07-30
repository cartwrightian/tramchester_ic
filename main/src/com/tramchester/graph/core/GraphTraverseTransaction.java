package com.tramchester.graph.core;

import org.neo4j.graphdb.Path;

public interface GraphTraverseTransaction {
    GraphNode fromEnd(Path path);
}
