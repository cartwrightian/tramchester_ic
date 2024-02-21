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
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Direction;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;

public class TraversalOps {
    private final NodeContentsRepository nodeOperations;
    private final TripRepository tripRepository;
    private final IdSet<Station> destinationStationIds;
    private final IdSet<Route> destinationRoutes;
    private final TramDate queryDate;
    private final GraphTransaction txn;
    private final int queryHour;

    private static final EnumSet<TransportRelationshipTypes> haveStationId = EnumSet.of(LEAVE_PLATFORM, INTERCHANGE_DEPART,
            DEPART, WALKS_TO_STATION, DIVERSION_DEPART);

    // TODO Split into fixed and journey specific, inject fixed direct into builders
    public TraversalOps(GraphTransaction txn, NodeContentsRepository nodeOperations, TripRepository tripRepository,
                        LocationSet destinations, TramDate queryDate, TramTime queryTime) {
        this.txn = txn;
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.destinationStationIds = destinations.stationsOnlyStream().collect(IdSet.collector());
        this.destinationRoutes = destinations.stream().
                flatMap(station -> station.getDropoffRoutes().stream()).
                collect(IdSet.collector());
        this.queryDate = queryDate;
        this.queryHour = queryTime.getHourOfDay();
    }

    public <R extends GraphRelationship> OptionalResourceIterator<R> getTowardsDestination(final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> destinationStationIds.contains(getLocationIdFor(depart))).
                collect(Collectors.toList());
        return OptionalResourceIterator.from(filtered);
    }

    private static IdFor<Station> getLocationIdFor(final GraphRelationship depart) {
        final TransportRelationshipTypes departType = depart.getType();
        if (haveStationId.contains(departType)) {
            return depart.getStationId();
        } else {
            throw new RuntimeException("Unsupported relationship type " + departType);
        }
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

    public TramTime getTimeFrom(GraphNode node) {
        return nodeOperations.getTime(node);
    }

    public Trip getTrip(IdFor<Trip> tripId) {
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

    public TramDate getQueryDate() {
        return queryDate;
    }

    public GraphTransaction getTransaction() {
        return txn;
    }

    public int getQueryHour() {
        return queryHour;
    }
}
