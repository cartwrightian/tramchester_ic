package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.facade.GraphTransaction;

@Deprecated
public class TraversalOps {
    private final GraphTransaction txn;

    public TraversalOps(GraphTransaction txn) {
        this.txn = txn;
    }

//    public TramTime getTimeFrom(final GraphNode node) {
//        return nodeOperations.getTime(node);
//    }

//    public Trip getTrip(final IdFor<Trip> tripId) {
//        return tripRepository.getTripById(tripId);
//    }

//    public boolean hasOutboundTripFor(final GraphNode node, final IdFor<Trip> tripId) {
//        return node.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).anyMatch(relationship -> relationship.hasTripIdInList(tripId));
//    }

}
