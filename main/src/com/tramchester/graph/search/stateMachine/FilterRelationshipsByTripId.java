package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class FilterRelationshipsByTripId {

    private final IdFor<Trip> tripId;

    public FilterRelationshipsByTripId(final IdFor<Trip> tripId) {
        this.tripId = tripId;
    }

    public Stream<ImmutableGraphRelationship> apply(final GraphTransaction txn, final GraphNode node) {
        return node.getRelationships(txn, OUTGOING, TO_SERVICE).
                filter(relationship -> relationship.hasTripIdInList(tripId));
    }
}
