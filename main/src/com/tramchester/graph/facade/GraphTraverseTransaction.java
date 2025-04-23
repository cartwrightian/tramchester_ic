package com.tramchester.graph.facade;

import org.neo4j.graphdb.Path;

public interface GraphTraverseTransaction {
    ImmutableGraphNode fromEnd(Path path);
    ImmutableGraphNode fromStart(Path path);
    ImmutableGraphRelationship lastFrom(Path path);
}
