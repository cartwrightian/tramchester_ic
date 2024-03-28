package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.FilterByDestinations;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;

import java.time.Duration;
import java.util.stream.Stream;

public class RouteStationStateEndTrip extends RouteStationState {

    // left a trip at the very end

    public static class Builder extends TowardsRouteStation<RouteStationStateEndTrip> {

        public Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
        }

        @Override
        public void register(final RegistersFromState registers) {
            registers.add(TraversalStateType.MinuteState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.RouteStationStateEndTrip;
        }

        public RouteStationStateEndTrip fromMinuteState(JourneyStateUpdate journeyState, final MinuteState minuteState, final GraphNode node,
                                                        final Duration cost, final boolean isInterchange, final GraphTransaction txn) {
            final TransportMode transportMode = node.getTransportMode();

            final FilterByDestinations<ImmutableGraphRelationship> towardsDestination = getTowardsDestination(node, txn);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return new RouteStationStateEndTrip(journeyState, minuteState, towardsDestination.stream(), cost, transportMode, node, this);
            }

            final Stream<ImmutableGraphRelationship> outboundsToFollow = getOutboundsToFollow(node, isInterchange, txn);

            return new RouteStationStateEndTrip(journeyState, minuteState, outboundsToFollow, cost, transportMode, node, this);
        }

    }

    private final TransportMode mode;
    private final GraphNode routeStationNode;

    private RouteStationStateEndTrip(JourneyStateUpdate journeyState, final ImmutableTraversalState minuteState,
                                     final Stream<ImmutableGraphRelationship> routeStationOutbound, final Duration cost,
                                     final TransportMode mode, final GraphNode routeStationNode,
                                     final TowardsRouteStation<RouteStationStateEndTrip> builder) {
        super(minuteState, routeStationOutbound, journeyState, cost, builder, routeStationNode);
        this.mode = mode;
        this.routeStationNode = routeStationNode;
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyStateUpdate) {
        leaveVehicle(journeyStateUpdate);
        return towardsStation.fromRouteStationEndTrip(this, node, cost, journeyStateUpdate, txn);
    }

    @Override
    protected TraversalState toPlatform(final PlatformState.Builder towardsPlatform, final GraphNode node,
                                        final Duration cost, final JourneyStateUpdate journeyState) {
        leaveVehicle(journeyState);
        return towardsPlatform.fromRouteStationEndTrip(this, node, cost, journeyState, txn);
    }

    private void leaveVehicle(final JourneyStateUpdate journeyState) {
        try {
            journeyState.leave(mode, getTotalDuration(), routeStationNode);
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to leave " + mode + " at " + this, e );
        }
    }
    @Override
    public String toString() {
        return "RouteStationStateEndTrip{" +
                "mode=" + mode +
                "} " + super.toString();
    }
}
