package com.tramchester.graph.search.stateMachine;

import com.google.common.collect.Streams;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final IdSet<Station> destinationStationIds;
    private final Set<Long> destinationNodeIds;
    private final LatLong destinationLatLon;
    private final SortsPositions sortsPositions;

    public TraversalOps(NodeContentsRepository nodeOperations, TripRepository tripRepository,
                        SortsPositions sortsPositions, Set<Station> destinationStations, Set<Long> destinationNodeIds,
                        LatLong destinationLatLon) {
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.sortsPositions = sortsPositions;
        this.destinationNodeIds = destinationNodeIds;
        this.destinationStationIds = destinationStations.stream().collect(IdSet.collector());
        this.destinationLatLon = destinationLatLon;
    }

    public List<Relationship> getTowardsDestination(Iterable<Relationship> outgoing) {
        return Streams.stream(outgoing).
                filter(depart -> destinationStationIds.contains(GraphProps.getStationIdFrom(depart))).
                collect(Collectors.toList());
    }

    public boolean isDestination(long nodeId) {
        return destinationNodeIds.contains(nodeId);
    }

    public IdFor<Service> getServiceIdFor(Node svcNode) {
        return nodeOperations.getServiceId(svcNode);
    }

    public Iterable<Relationship> filterBySingleTripId(Iterable<Relationship> relationships, IdFor<Trip> existingTripId) {
        return Streams.stream(relationships).
                filter(relationship -> nodeOperations.getTrip(relationship).equals(existingTripId)).
                collect(Collectors.toList());
    }

    public Stream<Relationship> orderRelationshipsByDistance(Iterable<Relationship> relationships) {
        Set<SortsPositions.HasStationId<Relationship>> wrapped = new HashSet<>();
        relationships.forEach(svcRelationship -> wrapped.add(new RelationshipFacade(svcRelationship)));
        return sortsPositions.sortedByNearTo(destinationLatLon, wrapped);
    }

    public TramTime getTimeFrom(Node node) {
        return nodeOperations.getTime(node);
    }

    public Trip getTrip(IdFor<Trip> tripId) {
        return tripRepository.getTripById(tripId);
    }

    public List<Relationship> filterByServiceId(Iterable<Relationship> relationships, IdFor<Service> svcId) {
        return Streams.stream(relationships).
                filter(relationship -> serviceNodeMatches(relationship, svcId)).
                collect(Collectors.toList());
    }

    private boolean serviceNodeMatches(Relationship relationship, IdFor<Service> currentSvcId) {
        // TODO Add ServiceID to Service Relationship??
        Node svcNode = relationship.getEndNode();
        IdFor<Service> svcId = getServiceIdFor(svcNode);
        return currentSvcId.equals(svcId);
    }

    public boolean hasOutboundFor(Node node, IdFor<Service> serviceId) {
        return Streams.stream(node.getRelationships(Direction.OUTGOING, TO_SERVICE)).
                anyMatch(relationship -> serviceNodeMatches(relationship, serviceId));
    }

    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;
        private final Long id;
        private final IdFor<Station> stationId;

        private RelationshipFacade(Relationship relationship) {
            id = relationship.getId();
            this.relationship = relationship;

            // TODO this needs to go via the cache layer
            this.stationId = GraphProps.getTowardsStationIdFrom(relationship.getEndNode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RelationshipFacade that = (RelationshipFacade) o;

            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public IdFor<Station> getStationId() {
            return stationId;
        }

        @Override
        public Relationship getContained() {
            return relationship;
        }
    }
}