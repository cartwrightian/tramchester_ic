package com.tramchester.graph.search.selectors;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.BranchSelector;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.TraversalContext;

import java.util.*;

public class BreadthFirstBranchSelectorForGridSearch implements BranchSelector {

    private final TraversalBranchQueue expansionQueue;
    private final PathExpander<JourneyState> expander;

    private TraversalBranch branchToExpand;

    public BreadthFirstBranchSelectorForGridSearch(TraversalBranch startBranch, PathExpander<JourneyState> expander,
                                                   StationsBoxSimpleGrid destinationBox, List<StationsBoxSimpleGrid> startingBoxes) {
        this.branchToExpand = startBranch;
        this.expander = expander;
        expansionQueue = new TraversalBranchQueue(destinationBox, startingBoxes);
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

    private static class TraversalBranchQueue {
        private final PriorityQueue<TraversalBranch> theQueue;
        private final Map<IdFor<Station>, StationsBoxSimpleGrid> stationToBox;
        private final StationsBoxSimpleGrid destination;

        public TraversalBranchQueue(StationsBoxSimpleGrid destination, final List<StationsBoxSimpleGrid> startingBoxes) {
            this.destination = destination;
            theQueue = new PriorityQueue<>(new BranchComparator());
            stationToBox = new HashMap<>();
            startingBoxes.forEach(box -> box.getStations().stream().forEach(station -> stationToBox.put(station.getId(), box)));
        }

        public TraversalBranch removeFront() {
            return theQueue.poll();
        }

        public void addBranch(final TraversalBranch next) {
            theQueue.add(next);
        }

        private class BranchComparator implements Comparator<TraversalBranch> {

            @Override
            public int compare(final TraversalBranch branchA, final TraversalBranch branchB) {
                final ImmutableJourneyState stateA = (JourneyState) branchA.state();
                final ImmutableJourneyState stateB = (JourneyState) branchB.state();

                final ImmutableTraversalState traversalStateA = stateA.getTraversalState();
                final ImmutableTraversalState traversalStateB = stateB.getTraversalState();

                // only worth comparing on distance if not the same node
                if (!traversalStateA.nodeId().equals(traversalStateB.nodeId())) {
                    if(stateA.hasBegunJourney() && stateB.hasBegunJourney()) {
                        final StationsBoxSimpleGrid boxA = stationToBox.get(stateA.approxPosition());
                        final StationsBoxSimpleGrid boxB = stationToBox.get(stateB.approxPosition());

                        return Integer.compare(distance(boxA), distance(boxB));
                    }
                }
                return stateA.getJourneyClock().compareTo(stateB.getJourneyClock());
            }

            private int distance(final StationsBoxSimpleGrid current) {
                final int distX = Math.abs(current.getX() - destination.getX());
                final int distY = Math.abs(current.getY() - destination.getY());
                return (distX*distX) + (distY*distY);
            }

        }
    }
}
