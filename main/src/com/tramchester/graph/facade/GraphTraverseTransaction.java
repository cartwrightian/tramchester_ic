package com.tramchester.graph.facade;

import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;
import org.neo4j.graphdb.Path;

public interface GraphTraverseTransaction {
    ImmutableGraphNode fromEnd(Path path);
    ImmutableGraphNode fromStart(Path path);
    ImmutableGraphRelationship lastFrom(Path path);
}
