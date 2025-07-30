package com.tramchester.graph.facade;

import org.neo4j.graphdb.Path;

public interface GraphTraverseTransaction {
    GraphNode fromEnd(Path path);
}
