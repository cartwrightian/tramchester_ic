package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.OptionalResourceIterator;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateOnTrip extends RouteStationState implements NodeId {

    private final IdFor<Trip> tripId;
    private final TransportMode transportMode;
    private final GraphNode routeStationNode;

    public static class Builder extends TowardsRouteStation<RouteStationStateOnTrip> {

        private final NodeContentsRepository nodeContents;

        public Builder(boolean interchangesOnly, NodeContentsRepository nodeContents) {
            super(interchangesOnly);
            this.nodeContents = nodeContents;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.MinuteState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.RouteStationStateOnTrip;
        }

        public RouteStationStateOnTrip fromMinuteState(MinuteState minuteState, GraphNode node, Duration cost,
                                                       boolean isInterchange, Trip trip, GraphTransaction txn) {
            // todo, use label and/or cache this - perf impact currently low
            TransportMode transportMode = GraphProps.getTransportMode(node);

            // TODO Crossing midnight?
            TramDate date = minuteState.traversalOps.getQueryDate();

            OptionalResourceIterator<GraphRelationship> towardsDestination = getTowardsDestination(minuteState.traversalOps, node, date, txn);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return new RouteStationStateOnTrip(minuteState, towardsDestination.stream(), cost, node, trip.getId(), transportMode, this);
            }

            // outbound service relationships that continue the current trip
            Stream<GraphRelationship> towardsServiceForTrip = filterByTripId(node.getRelationships(txn, OUTGOING, TO_SERVICE), trip, txn);

            // now add outgoing to platforms/stations
            Stream<GraphRelationship> outboundsToFollow = getOutboundsToFollow(node, isInterchange, date, txn);

            // NOTE: order of the concatenation matters here for depth first, need to do departs first to
            // explore routes including changes over continuing on possibly much longer trip
            final Stream<GraphRelationship> relationships = Stream.concat(outboundsToFollow, towardsServiceForTrip);
            return new RouteStationStateOnTrip(minuteState, relationships, cost, node, trip.getId(), transportMode, this);
        }

        private Stream<GraphRelationship> filterByTripId(Stream<GraphRelationship> svcRelationships, Trip trip, GraphTransaction txn) {
            IdFor<Service> currentSvcId = trip.getService().getId();
            return svcRelationships.
                    filter(relationship -> currentSvcId.equals(nodeContents.getServiceId(relationship.getEndNode(txn))));
        }

    }

    private RouteStationStateOnTrip(TraversalState parent, Stream<GraphRelationship> relationships, Duration cost,
                                    GraphNode routeStationNode, IdFor<Trip> tripId, TransportMode transportMode, TowardsRouteStation<RouteStationStateOnTrip> builder) {
        super(parent, relationships, cost, builder);
        this.routeStationNode = routeStationNode;
        this.tripId = tripId;
        this.transportMode = transportMode;
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, GraphNode node, Duration cost) {
        return towardsService.fromRouteStation(this, tripId, node, cost, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, GraphNode node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        leaveVehicle(journeyState, transportMode, "Unable to depart tram");
        return towardsNoPlatformStation.fromRouteStation(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, GraphNode node, Duration cost,
                                        JourneyStateUpdate journeyState) {

        leaveVehicle(journeyState, transportMode, "Unable to process platform");
        return towardsPlatform.fromRouteStationOnTrip(this, node, cost, txn);
    }

    private void leaveVehicle(JourneyStateUpdate journeyState, TransportMode transportMode, String diag) {
        try {
            journeyState.leave(tripId, transportMode, getTotalDuration(), routeStationNode);
        } catch (TramchesterException e) {
            throw new RuntimeException(diag, e);
        }
    }

    @Override
    public GraphNodeId nodeId() {
        return routeStationNode.getId();
    }

    @Override
    public String toString() {
        return "RouteStationStateOnTrip{" +
                "routeStationNodeId=" + routeStationNode.getId() +
                ", tripId=" + tripId +
                ", transportMode=" + transportMode +
                "} " + super.toString();
    }


}
