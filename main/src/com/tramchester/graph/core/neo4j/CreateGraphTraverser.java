package com.tramchester.graph.core.neo4j;

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

public interface CreateGraphTraverser {
    Traverser getTraverser(final TraversalDescription traversalDesc);
}
