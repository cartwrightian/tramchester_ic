package com.tramchester.graph.search.selectors;

import com.tramchester.domain.LocationCollection;
import com.tramchester.geo.LocationDistances;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.BranchSelector;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.TraversalContext;

import java.util.Comparator;
import java.util.PriorityQueue;

public class BreadthFirstBranchSelector implements BranchSelector {

    private final TraversalBranchQueue expansionQueue;
    private final PathExpander<JourneyState> expander;

    private TraversalBranch branchToExpand;

    public BreadthFirstBranchSelector(final TraversalBranch start, final PathExpander<JourneyState> expander,
                                      final LocationDistances locationDistances, final LocationCollection destinationIds) {
        this.branchToExpand = start;
        this.expander = expander;
        expansionQueue = new TraversalBranchQueue(locationDistances, destinationIds);
    }

    // breadth
    @Override
    public TraversalBranch next(final TraversalContext metadata) {
        TraversalBranch next = branchToExpand.next(expander, metadata);
        while (next!=null) {
            expansionQueue.addBranch(next);
            next = branchToExpand.next(expander, metadata);
        }
        branchToExpand = expansionQueue.removeFront();
        return branchToExpand;
    }

    public static class TraversalBranchQueue {
        private final PriorityQueue<TraversalBranch> theQueue;
        private final LocationDistances.FindDistancesTo findDistances;

        public TraversalBranchQueue(final LocationDistances locationDistances, final LocationCollection destinations) {
            findDistances = locationDistances.findDistancesTo(destinations);
            theQueue = new PriorityQueue<>(new BranchComparator());
        }

        public TraversalBranch removeFront() {
            return theQueue.poll();
        }

        public void addBranch(final TraversalBranch next) {
            theQueue.add(next);
        }

        private class BranchComparator implements Comparator<TraversalBranch> {

            // TODO What is the right Strategy here?
            // Comparing only on distance once seen a station, so for majority of time

            @Override
            public int compare(final TraversalBranch branchA, final TraversalBranch branchB) {
                // GraphState -> JourneyState -> TraversalState
                final ImmutableJourneyState journeyStateA = (ImmutableJourneyState) branchA.state();
                final ImmutableJourneyState journeyStateB = (ImmutableJourneyState) branchB.state();

                // only worth comparing on distance if not the same node
                if (journeyStateA.getNodeId().equals(journeyStateB.getNodeId())) {
                    return journeyStateA.getJourneyClock().compareTo(journeyStateB.getJourneyClock());
                }

                if(journeyStateA.hasBegunJourney() && journeyStateB.hasBegunJourney()) {
                    return findDistances.compare(journeyStateA.approxPosition(), journeyStateB.approxPosition());
                } else if (journeyStateA.hasBegunJourney()) {
                    return -1;
                } else if (journeyStateB.hasBegunJourney()) {
                    return 1;
                } else {
                    return journeyStateA.getJourneyClock().compareTo(journeyStateB.getJourneyClock());
                }

            }

        }
    }

}
