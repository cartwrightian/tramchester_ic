package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Direction;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final GraphTransaction txn;

    public TraversalOps(GraphTransaction txn, NodeContentsRepository nodeOperations, TripRepository tripRepository) {
        this.txn = txn;
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
    }

    public TramTime getTimeFrom(final GraphNode node) {
        return nodeOperations.getTime(node);
    }

    public Trip getTrip(final IdFor<Trip> tripId) {
        return tripRepository.getTripById(tripId);
    }

    private boolean serviceNodeMatches(final GraphRelationship relationship, final IdFor<Service> currentSvcId) {
        // TODO Add ServiceID to Service Relationship??
        final GraphNode svcNode = relationship.getEndNode(txn);
        final IdFor<Service> svcId = nodeOperations.getServiceId(svcNode);
        return currentSvcId.equals(svcId);
    }

    public boolean hasOutboundFor(final GraphNode node, final IdFor<Service> serviceId) {
        return node.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).anyMatch(relationship -> serviceNodeMatches(relationship, serviceId));
    }

}
