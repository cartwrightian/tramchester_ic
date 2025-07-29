package com.tramchester.graph.facade;

import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;
import org.neo4j.graphdb.Path;

public interface GraphTraverseTransaction {
    ImmutableGraphNode fromEnd(Path path);
}
