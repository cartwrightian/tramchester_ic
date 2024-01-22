package com.tramchester.graph.search;

import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.BranchSelector;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.TraversalContext;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class SpikeBranchSelector implements BranchSelector {

    private final TraversalBranchQueue expansionQueue;
    private TraversalBranch branchToExpand;
    private final PathExpander<JourneyState> expander;

    public SpikeBranchSelector(final TraversalBranch start, final PathExpander<JourneyState> expander) {
        this.branchToExpand = start;
        this.expander = expander;
        expansionQueue = new TraversalBranchQueue();
    }

    // breadth
    @Override
    public TraversalBranch next(final TraversalContext metadata) {
        TraversalBranch result = null;
        while (result == null) {
            final TraversalBranch next = branchToExpand.next(expander, metadata);
            if (next != null) {
                expansionQueue.addBranch(next);
                result = next;
            } else {
                // nothing more from current branchToExpand, so revert to next in queue
                branchToExpand = expansionQueue.removeFront();
                if (branchToExpand == null) {
                    return null;
                }
            }
        }
        return result;
    }

    private static class TraversalBranchQueue {
        private final PriorityQueue<TraversalBranch> theQueue;

        private TraversalBranchQueue() {
            theQueue = new PriorityQueue<>(new BranchComparator());
        }

        public TraversalBranch removeFront() {
            return theQueue.poll();
        }

        public void addBranch(TraversalBranch next) {
            theQueue.add(next);
        }

        private static class BranchComparator implements Comparator<TraversalBranch> {
            @Override
            public int compare(final TraversalBranch branchA, final TraversalBranch branchB) {
                final JourneyState stateA = (JourneyState) branchA.state();
                final JourneyState stateB = (JourneyState) branchB.state();
                final int compare =  stateA.getJourneyClock().compareTo(stateB.getJourneyClock());
                if (compare==0) {
                    return Integer.compare(stateA.getNumberChanges(), stateB.getNumberChanges());
                } else {
                    return compare;
                }
            }
        }
    }

    private static class SimpleTraversalBranchQueue {
        private final Queue<TraversalBranch> theQueue;

        private SimpleTraversalBranchQueue() {
            theQueue = new LinkedList<>();
        }

        public void addBranch(final TraversalBranch traversalBranch) {
            theQueue.add(traversalBranch);
        }

        public TraversalBranch removeFront() {
            return theQueue.poll();
        }
    }


}
