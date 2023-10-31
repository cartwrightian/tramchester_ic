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
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.RelationshipWithRoute;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Direction;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final IdSet<Station> destinationStationIds;
    private final IdSet<Route> destinationRoutes;
    private final LatLong destinationLatLon;
    private final SortsPositions sortsPositions;
    private final LowestCostsForDestRoutes lowestCostsForRoutes;
    private final TramDate queryDate;

    // TODO Split into fixed and journey specific, inject fixed direct into builders
    public TraversalOps(NodeContentsRepository nodeOperations, TripRepository tripRepository,
                        SortsPositions sortsPositions, LocationSet destinations,
                        LatLong destinationLatLon, LowestCostsForDestRoutes lowestCostsForRoutes,
                        TramDate queryDate) {
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.sortsPositions = sortsPositions;
        this.destinationStationIds = destinations.stationsOnlyStream().collect(IdSet.collector());
        this.destinationRoutes = destinations.stream().
                flatMap(station -> station.getDropoffRoutes().stream()).
                collect(IdSet.collector());
        this.destinationLatLon = destinationLatLon;
        this.lowestCostsForRoutes = lowestCostsForRoutes;
        this.queryDate = queryDate;
    }

//    @Deprecated
//    public OptionalResourceIterator<Relationship> getTowardsDestination(ResourceIterable<Relationship> outgoing) {
//        return getTowardsDestination(outgoing.stream());
//    }

    public OptionalResourceIterator<GraphRelationship> getTowardsDestination(Stream<GraphRelationship> outgoing) {
        List<GraphRelationship> filtered = outgoing.
                filter(depart -> destinationStationIds.contains(depart.getStationId())). // GraphProps.getStationIdFrom(depart))).
                collect(Collectors.toList());
        return OptionalResourceIterator.from(filtered);
    }

    public int onDestRouteFirst(HasId<Route> a, HasId<Route> b) {
        IdFor<Route> routeA = a.getId();
        IdFor<Route> routeB = b.getId();
        boolean toDestA = destinationRoutes.contains(routeA);
        boolean toDestB = destinationRoutes.contains(routeB);
        if (toDestA == toDestB) {
            return 0;
        }
        if (toDestA) {
            return -1;
        }
        return 1;
    }

    public Stream<GraphRelationship> orderRelationshipsByDistance(Stream<GraphRelationship> relationships) {
//        Set<SortsPositions.HasStationId<GraphRelationship>> wrapped = new HashSet<>();
//        relationships.forEach(svcRelationship -> wrapped.add(new RelationshipFacade(svcRelationship)));

        Set<SortsPositions.HasStationId<GraphRelationship>> wrapped = relationships.map(RelationshipFacade::new).collect(Collectors.toSet());
        return sortsPositions.sortedByNearTo(destinationLatLon, wrapped);
    }

    public Stream<GraphRelationship> orderBoardingRelationsByDestRoute(Stream<GraphRelationship> relationships) {
        return relationships.map(RelationshipWithRoute::new).
                sorted(this::onDestRouteFirst).
                map(RelationshipWithRoute::getRelationship);
    }

    public Stream<GraphRelationship> orderBoardingRelationsByRouteConnections(Stream<GraphRelationship> toServices) {
        Stream<RelationshipWithRoute> withRouteId = toServices.map(RelationshipWithRoute::new);
        Stream<RelationshipWithRoute> sorted = lowestCostsForRoutes.sortByDestinations(withRouteId);
        return sorted.map(RelationshipWithRoute::getRelationship);
    }

    public TramTime getTimeFrom(GraphNode node) {
        return nodeOperations.getTime(node);
    }

    public Trip getTrip(IdFor<Trip> tripId) {
        return tripRepository.getTripById(tripId);
    }

    private boolean serviceNodeMatches(GraphRelationship relationship, IdFor<Service> currentSvcId) {
        // TODO Add ServiceID to Service Relationship??
        GraphNode svcNode = relationship.getEndNode();
        IdFor<Service> svcId = nodeOperations.getServiceId(svcNode);
        return currentSvcId.equals(svcId);
    }

    public boolean hasOutboundFor(GraphNode node, IdFor<Service> serviceId) {
        return node.getRelationships(Direction.OUTGOING, TO_SERVICE).anyMatch(relationship -> serviceNodeMatches(relationship, serviceId));
    }

    public TramDate getQueryDate() {
        return queryDate;
    }

    private static class RelationshipFacade implements SortsPositions.HasStationId<GraphRelationship> {
        private final GraphRelationship relationship;
        private final Long id;
        private final IdFor<Station> stationId;

        private RelationshipFacade(GraphRelationship relationship) {
            id = relationship.getIdOLD();
            this.relationship = relationship;

            // TODO this needs to go via the cache layer?
            this.stationId = relationship.getEndNode().getStationId(); // GraphProps.getTowardsStationIdFrom(relationship.getEndNode());
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
        public GraphRelationship getContained() {
            return relationship;
        }
    }

}
