package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;

import java.util.stream.Stream;

public class GetOutgoingServicesMatchingTripId {

    private final IdFor<Trip> tripId;

    public GetOutgoingServicesMatchingTripId(final IdFor<Trip> tripId) {
        this.tripId = tripId;
    }

    public Stream<GraphRelationship> apply(final GraphTransaction txn, final GraphNode node) {
        return node.getOutgoingServiceMatching(txn, tripId);
//        return node.getRelationships(txn, OUTGOING, TO_SERVICE).
//                filter(relationship -> relationship.hasTripIdInList(tripId));
    }
}
