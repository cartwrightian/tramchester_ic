package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationshipNeo4J;

import java.util.stream.Stream;

public class GetOutgoingServicesMatchingTripId {

    private final IdFor<Trip> tripId;

    public GetOutgoingServicesMatchingTripId(final IdFor<Trip> tripId) {
        this.tripId = tripId;
    }

    public Stream<ImmutableGraphRelationship> apply(final GraphTransactionNeo4J txn, final GraphNode node) {
        return node.getOutgoingServiceMatching(txn, tripId);
//        return node.getRelationships(txn, OUTGOING, TO_SERVICE).
//                filter(relationship -> relationship.hasTripIdInList(tripId));
    }
}
