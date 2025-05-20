package com.tramchester.graph.search.selectors;

import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.BranchSelector;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.TraversalContext;

public class SimpleDepthFirstBranchSelector implements BranchSelector {
    private TraversalBranch currentPosition;

    private final PathExpander<JourneyState> expander;

    SimpleDepthFirstBranchSelector(TraversalBranch startSource, PathExpander<JourneyState> expander) {
        this.currentPosition = startSource;
        this.expander = expander;
    }

    @Override
    public TraversalBranch next(final TraversalContext metadata) {
        TraversalBranch result = null;
        while (result == null) {
            if (currentPosition == null) {
                return null;
            }

            final TraversalBranch next = currentPosition.next(expander, metadata);
            if (next == null) {
                // no more available at this level so step back up one level
                currentPosition = currentPosition.parent();
            } else {
                // immediately step down to next level
                currentPosition = next;
                result = next;
            }
        }
        return result;
    }
}
