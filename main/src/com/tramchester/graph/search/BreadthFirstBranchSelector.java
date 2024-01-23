package com.tramchester.graph.search;

import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationDistances;
import com.tramchester.graph.search.stateMachine.states.StationState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
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
                                      final StationDistances stationDistances, LocationSet destinationIds) {
        this.branchToExpand = start;
        this.expander = expander;
        expansionQueue = new TraversalBranchQueue(stationDistances, destinationIds);
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
        private final StationDistances.FindDistancesTo findDistances;

        private TraversalBranchQueue(StationDistances stationDistances, LocationSet destinations) {
            findDistances = stationDistances.findDistancesTo(destinations);
            theQueue = new PriorityQueue<>(new BranchComparator());
        }

        public TraversalBranch removeFront() {
            return theQueue.poll();
        }

        public void addBranch(TraversalBranch next) {
            theQueue.add(next);
        }

        private class BranchComparator implements Comparator<TraversalBranch> {

            // TODO What is the right Strategy here?

            // just copy a-star? at least for StationState

            @Override
            public int compare(final TraversalBranch branchA, final TraversalBranch branchB) {
                final JourneyState stateA = (JourneyState) branchA.state();
                final JourneyState stateB = (JourneyState) branchB.state();
                final TraversalState traversalStateA = stateA.getTraversalState();
                final TraversalState traversalStateB = stateB.getTraversalState();
                if (!traversalStateA.nodeId().equals(traversalStateB.nodeId())) {
                    // only worth comparing on distance if not the same node
                    if ((traversalStateA instanceof StationState stationStateA) && (traversalStateB instanceof StationState stationStateB)) {

                        final IdFor<Station> stationIdA = stationStateA.getStationId();
                        final IdFor<Station> stationIdB = stationStateB.getStationId();

                        long distanceA = findDistances.toStation(stationIdA);
                        long distanceB = findDistances.toStation(stationIdB);

                        return Long.compare(distanceA, distanceB);

                    }
                }
                return stateA.getJourneyClock().compareTo(stateB.getJourneyClock());
            }
        }
    }

}
