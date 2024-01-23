package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Direction;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final IdSet<Station> destinationStationIds;
    private final IdSet<Route> destinationRoutes;
//    private final LatLong destinationLatLon;
//    private final SortsPositions sortsPositions;
//    private final LowestCostsForDestRoutes lowestCostsForRoutes;
    private final TramDate queryDate;
    private final GraphTransaction txn;

    // TODO Split into fixed and journey specific, inject fixed direct into builders
    public TraversalOps(GraphTransaction txn, NodeContentsRepository nodeOperations, TripRepository tripRepository,
                        LocationSet destinations, LowestCostsForDestRoutes lowestCostsForRoutes,
                        TramDate queryDate) {
        this.txn = txn;
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.destinationStationIds = destinations.stationsOnlyStream().collect(IdSet.collector());
        this.destinationRoutes = destinations.stream().
                flatMap(station -> station.getDropoffRoutes().stream()).
                collect(IdSet.collector());
//        this.destinationLatLon = destinationLatLon;
//        this.lowestCostsForRoutes = lowestCostsForRoutes;
//        this.sortsPositions = sortsPositions;
        this.queryDate = queryDate;
    }

    public <R extends GraphRelationship> OptionalResourceIterator<R> getTowardsDestination(final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> destinationStationIds.contains(depart.getStationId())).
                collect(Collectors.toList());
        return OptionalResourceIterator.from(filtered);
    }

    public int onDestRouteFirst(final HasId<Route> a, final HasId<Route> b) {
        final IdFor<Route> routeA = a.getId();
        final IdFor<Route> routeB = b.getId();
        final boolean toDestA = destinationRoutes.contains(routeA);
        final boolean toDestB = destinationRoutes.contains(routeB);
        if (toDestA == toDestB) {
            return 0;
        }
        if (toDestA) {
            return -1;
        }
        return 1;
    }

//    public Stream<GraphRelationship> orderRelationshipsByDistance(Stream<ImmutableGraphRelationship> relationships) {
//
//        Set<SortsPositions.HasStationId<GraphRelationship>> facades = relationships.
//                map(RelationshipFacade::new).
//                collect(Collectors.toSet());
//        return sortsPositions.sortedByNearTo(destinationLatLon, facades);
//    }
//

//    public Stream<ImmutableGraphRelationship> orderBoardingRelationsByRouteConnections(Stream<ImmutableGraphRelationship> relationships) {
//        return lowestCostsForRoutes.
//                sortByDestinations(relationships.map(RelationshipWithRoute::new)).
//                map(RelationshipWithRoute::getRelationship);
//    }

    public TramTime getTimeFrom(GraphNode node) {
        return nodeOperations.getTime(node);
    }

    public Trip getTrip(IdFor<Trip> tripId) {
        return tripRepository.getTripById(tripId);
    }

    private boolean serviceNodeMatches(GraphRelationship relationship, IdFor<Service> currentSvcId) {
        // TODO Add ServiceID to Service Relationship??
        GraphNode svcNode = relationship.getEndNode(txn);
        IdFor<Service> svcId = nodeOperations.getServiceId(svcNode);
        return currentSvcId.equals(svcId);
    }

    public boolean hasOutboundFor(GraphNode node, IdFor<Service> serviceId) {
        return node.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).anyMatch(relationship -> serviceNodeMatches(relationship, serviceId));
    }

    public TramDate getQueryDate() {
        return queryDate;
    }

    public GraphTransaction getTransaction() {
        return txn;
    }

//    private static class RelationshipFacade implements SortsPositions.HasStationId<GraphRelationship> {
//        private final GraphRelationship relationship;
//        private final GraphRelationshipId id;
//        private final IdFor<Station> stationId;
//
//        private RelationshipFacade(final GraphRelationship relationship) {
//            this.id = relationship.getId();
//            this.relationship = relationship;
//
//            // TODO this needs to go via the cache layer?
//            this.stationId = relationship.getEndStationId(); //getEndNode(txn).getStationId(); // GraphProps.getTowardsStationIdFrom(relationship.getEndNode());
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            RelationshipFacade that = (RelationshipFacade) o;
//            return Objects.equals(id, that.id);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(id);
//        }
//
//        @Override
//        public IdFor<Station> getStationId() {
//            return stationId;
//        }
//
//        @Override
//        public GraphRelationship getContained() {
//            return relationship;
//        }
//    }

}
