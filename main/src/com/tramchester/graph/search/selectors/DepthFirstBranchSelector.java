package com.tramchester.graph.search.selectors;

import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.BranchSelector;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.TraversalContext;

import java.util.*;

public class DepthFirstBranchSelector implements BranchSelector {
    private TraversalBranch currentPosition;

    private final PathExpander<JourneyState> expander;
    private final PriorityQueue<TraversalBranch> queue;

    DepthFirstBranchSelector(TraversalBranch startSource, PathExpander<JourneyState> expander) {
        this.currentPosition = startSource;
        this.expander = expander;
        this.queue = new PriorityQueue<>(new ClockTimeComparator());
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
                //currentPosition = queue.poll();
            } else {
                // parent into queue
//                final TraversalBranch parent = next.parent();
//                if (parent!=null) {
//                    if (parent.includes()) {
//                        queue.add(parent);
//                    }
//                }

                // immediately step down to next level
                currentPosition = next;
                result = next;
            }
        }
        return result;
    }

//    TraversalBranch result = null;
//        while (result == null) {
//        if (current == null) {
//            return null;
//        }
//        TraversalBranch next = current.next(expander, metadata);
//        if (next == null) {
//            current = current.parent();
//            continue;
//        }
//        current = next;
//        result = current;
//    }
//        return result;

    private static class ClockTimeComparator implements Comparator<TraversalBranch> {
        @Override
        public int compare(final TraversalBranch branchA, final TraversalBranch branchB) {
            final JourneyState stateA = (JourneyState) branchA.state();
            final JourneyState stateB = (JourneyState) branchB.state();
            return stateA.getJourneyClock().compareTo(stateB.getJourneyClock());
        }
    }

}
