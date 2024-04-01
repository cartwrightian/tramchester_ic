package com.tramchester.graph.search.selectors;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.JourneyState;
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
            // GraphState -> JourneyState -> TraversalState

            @Override
            public int compare(final TraversalBranch branchA, final TraversalBranch branchB) {
                final ImmutableJourneyState journeyStateA = (JourneyState) branchA.state();
                final ImmutableJourneyState journeyStateB = (JourneyState) branchB.state();

//                final ImmutableTraversalState traversalStateA = journeyStateA.getTraversalState();
//                final ImmutableTraversalState traversalStateB = journeyStateB.getTraversalState();

                // only worth comparing on distance if not the same node
                if (!journeyStateA.getNodeId().equals(journeyStateB.getNodeId())) {
                    if(journeyStateA.hasBegunJourney() && journeyStateB.hasBegunJourney()) {
                        final IdFor<? extends Location<?>> approxPositionA = journeyStateA.approxPosition();
                        final IdFor<? extends Location<?>> approxPositionB = journeyStateB.approxPosition();

                        if (approxPositionA.getDomainType()== Station.class && approxPositionB.getDomainType()== Station.class) {
                            final StationsBoxSimpleGrid boxA = stationToBox.get(approxPositionA);
                            final StationsBoxSimpleGrid boxB = stationToBox.get(approxPositionB);

                            return Integer.compare(distance(boxA), distance(boxB));
                        }

                    }
                }
                return journeyStateA.getJourneyClock().compareTo(journeyStateB.getJourneyClock());
            }

            private int distance(final StationsBoxSimpleGrid current) {
                final int distX = Math.abs(current.getX() - destination.getX());
                final int distY = Math.abs(current.getY() - destination.getY());
                return (distX*distX) + (distY*distY);
            }

        }
    }
}
