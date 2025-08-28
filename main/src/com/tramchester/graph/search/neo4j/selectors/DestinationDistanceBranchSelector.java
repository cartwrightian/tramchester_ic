package com.tramchester.graph.search.neo4j.selectors;

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

public class DestinationDistanceBranchSelector implements BranchSelector {

    private final TraversalBranchQueue expansionQueue;
    private final PathExpander<JourneyState> expander;

    private TraversalBranch branchToExpand;

    public DestinationDistanceBranchSelector(final TraversalBranch start, final PathExpander<JourneyState> expander,
                                             final LocationDistances locationDistances, final LocationCollection destinationIds) {
        this.branchToExpand = start;
        this.expander = expander;
        expansionQueue = new TraversalBranchQueue(locationDistances, destinationIds);
    }

    @Override
    public TraversalBranch next(final TraversalContext metadata) {
        TraversalBranch next = branchToExpand.next(expander, metadata);
        while (next!=null) {
            // walk down the branch adding to queue
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

            @Override
            public int compare(final TraversalBranch branchA, final TraversalBranch branchB) {
                // GraphState -> JourneyState -> TraversalState
                final ImmutableJourneyState journeyStateA = (ImmutableJourneyState) branchA.state();
                final ImmutableJourneyState journeyStateB = (ImmutableJourneyState) branchB.state();

                return compareJourneyState(journeyStateA, journeyStateB);

            }

            private int compareJourneyState(final ImmutableJourneyState journeyStateA, final ImmutableJourneyState journeyStateB) {
                // only worth comparing on distance if not already at the same node
                if (journeyStateA.getNodeId().equals(journeyStateB.getNodeId())) {
                    // arbitrarily pick the earliest time
                    return journeyStateA.getJourneyClock().compareTo(journeyStateB.getJourneyClock());
                }

                // by distance, or prefer having starting journey, or fall back to clock time
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
