package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.GraphNodeId;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.*;

import java.time.Duration;
import java.util.stream.Stream;

public class RouteStationStateOnTrip extends RouteStationState implements NodeId {

    // left a trip part way through

    private final IdFor<Trip> tripId;
    private final TransportMode transportMode;
    private final GraphNode routeStationNode;

    public static class Builder extends TowardsRouteStation<RouteStationStateOnTrip> {

        public Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.MinuteState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.RouteStationStateOnTrip;
        }

        public RouteStationStateOnTrip fromMinuteState(final JourneyStateUpdate journeyState, final MinuteState minuteState,
                                                       final GraphNode routeStationNode, final Duration cost, final boolean isInterchange,
                                                       final GetOutgoingServicesMatchingTripId getOutgoingServicesMatchingTripId, // TODO Remove this
                                                       final GraphTransactionNeo4J txn) {
            // todo, use label and/or cache this - perf impact currently low
            final TransportMode transportMode = routeStationNode.getTransportMode();
            final IdFor<Trip> tripId = journeyState.getCurrentTrip();

            final FilterByDestinations<ImmutableGraphRelationship> towardsDestination = getTowardsDestination(routeStationNode, txn);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return new RouteStationStateOnTrip(journeyState, minuteState, towardsDestination.stream(), cost,
                        routeStationNode, tripId, transportMode, this);
            }

            // outbound service relationships that continue the current trip
            final Stream<ImmutableGraphRelationship> towardsServiceForTrip = getOutgoingServicesMatchingTripId.apply(txn, routeStationNode);

            // now add outgoing to platforms/stations
            final Stream<ImmutableGraphRelationship> outboundsToFollow = getOutboundsToFollow(routeStationNode, isInterchange, txn);

            // NOTE: order of the concatenation matters here for depth first, need to do departs first to
            // explore routes including changes over continuing on possibly much longer trip
            final Stream<ImmutableGraphRelationship> relationships = Stream.concat(outboundsToFollow, towardsServiceForTrip);
            return new RouteStationStateOnTrip(journeyState, minuteState, relationships, cost, routeStationNode, tripId, transportMode, this);
        }

    }

    private RouteStationStateOnTrip(JourneyStateUpdate journeyState, final ImmutableTraversalState parent, final Stream<ImmutableGraphRelationship> relationships,
                                    final Duration cost, final GraphNode routeStationNode, final IdFor<Trip> tripId, final TransportMode transportMode,
                                    final TowardsRouteStation<RouteStationStateOnTrip> builder) {
        super(parent, relationships, journeyState, cost, builder, routeStationNode);
        this.routeStationNode = routeStationNode;
        this.tripId = tripId;
        this.transportMode = transportMode;
    }

    @Override
    protected TraversalState toService(final ServiceState.Builder towardsService, final GraphNode serviceNode, final Duration cost) {
        return towardsService.fromRouteStation(this, serviceNode, cost, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder towardsNoPlatformStation, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState, transportMode, "Unable to depart tram");
        return towardsNoPlatformStation.fromRouteStationOnTrip(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toPlatform(final PlatformState.Builder towardsPlatform, final GraphNode node, final Duration cost,
                                        final JourneyStateUpdate journeyState) {

        leaveVehicle(journeyState, transportMode, "Unable to process platform");
        return towardsPlatform.fromRouteStationOnTrip(this, node, cost, journeyState, txn);
    }

    private void leaveVehicle(final JourneyStateUpdate journeyState, final TransportMode transportMode, final String diag) {
        try {
            journeyState.leave( transportMode, getTotalDuration(), routeStationNode);
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
