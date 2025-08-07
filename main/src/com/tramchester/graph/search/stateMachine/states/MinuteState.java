package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.GetOutgoingServicesMatchingTripId;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.stream.Stream;

public class MinuteState extends TraversalState implements HasTowardsStationId {

    public static class Builder extends StateBuilder<MinuteState> {
        final TransportRelationshipTypes[] currentModes;

        public Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
            currentModes = builderParameters.currentModes();
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.HourState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.MinuteState;
        }

        public TraversalState fromHour(final HourState hourState, final GraphNode minuteNode, final Duration cost,
                                       final IdFor<Station> towardsStationId, final JourneyStateUpdate journeyState,
                                       final GraphTransaction txn) {

            final Stream<GraphRelationship> allOutboundForMode = minuteNode.getRelationships(txn, GraphDirection.Outgoing, currentModes);

            if (journeyState.onTrip()) {
                final IdFor<Trip> existingTripId = journeyState.getCurrentTrip();
                final Stream<GraphRelationship> filterBySingleTripId = filterBySingleTripId(allOutboundForMode, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, minuteNode, journeyState, towardsStationId, cost, this);
            } else {
                // since at minute/trip node now have specific tripId to use
                final IdFor<Trip> newTripId = getTrip(minuteNode);
                journeyState.beginTrip(newTripId);
                return new MinuteState(hourState, allOutboundForMode, minuteNode, journeyState, towardsStationId, cost, this);
            }
        }

        private Stream<GraphRelationship> filterBySingleTripId(final Stream<GraphRelationship> relationships, final IdFor<Trip> existingTripId) {
            return relationships.filter(relationship -> relationship.getTripId().equals(existingTripId));
        }
    }

    private final IdFor<Station> towardsStationId;

    private MinuteState(final ImmutableTraversalState parent, final Stream<GraphRelationship> relationships, GraphNode node,
                        final JourneyStateUpdate journeyState, IdFor<Station> towardsStationId, final Duration cost,
                        final Towards<MinuteState> builder) {
        super(parent, relationships, cost, builder.getDestination(), node.getId());
        this.towardsStationId = towardsStationId;
    }

    private static IdFor<Trip> getTrip(final GraphNode node) {
        if (!node.hasTripId()) {
            throw new RuntimeException("Missing trip id at " + node);
        }
        return node.getTripId();
    }

    @Override
    public IdFor<Station> getTowards() {
        return towardsStationId;
    }

    @Override
    protected RouteStationStateOnTrip toRouteStationOnTrip(final RouteStationStateOnTrip.Builder towardsRouteStation, JourneyStateUpdate journeyState,
                                                           final GraphNode routeStationNode, final Duration cost, final boolean isInterchange) {

        final GetOutgoingServicesMatchingTripId getOutgoingServicesMatchingTripId = new GetOutgoingServicesMatchingTripId(journeyState.getCurrentTrip());
        return towardsRouteStation.fromMinuteState(journeyState, this, routeStationNode, cost, isInterchange, getOutgoingServicesMatchingTripId, txn);
    }

    @Override
    protected RouteStationStateEndTrip toRouteStationEndTrip(final RouteStationStateEndTrip.Builder towardsEndTrip,
                                                             final JourneyStateUpdate journeyState, final GraphNode routeStationNode,
                                                             final Duration cost, final boolean isInterchange) {

        return towardsEndTrip.fromMinuteState(journeyState, this, routeStationNode, cost, isInterchange, txn);
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "} " + super.toString();
    }

}
