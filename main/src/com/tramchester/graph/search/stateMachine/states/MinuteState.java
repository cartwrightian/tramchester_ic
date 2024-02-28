package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.stream.Stream;

import static org.neo4j.graphdb.Direction.OUTGOING;

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

        public TraversalState fromHour(final HourState hourState, final GraphNode node, final Duration cost, final ExistingTrip existingTrip,
                                       final IdFor<Station> towardsStationId, final JourneyStateUpdate journeyState,
                                       final GraphTransaction txn) {

            final Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, OUTGOING, currentModes);

            if (existingTrip.isOnTrip()) {
                final IdFor<Trip> existingTripId = existingTrip.getTripId();
                final Stream<ImmutableGraphRelationship> filterBySingleTripId = filterBySingleTripId(relationships, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, node, existingTripId, towardsStationId, cost, this);
            } else {
                // starting a brand-new journey, since at minute node now have specific tripid to use
                final IdFor<Trip> newTripId = getTrip(node);
                journeyState.beginTrip(newTripId);
                return new MinuteState(hourState, relationships, node, newTripId, towardsStationId, cost, this);
            }
        }

        private Stream<ImmutableGraphRelationship> filterBySingleTripId(final Stream<ImmutableGraphRelationship> relationships, final IdFor<Trip> existingTripId) {
            return relationships.filter(relationship -> super.getTripId(relationship).equals(existingTripId));
        }
    }

//    private final boolean interchangesOnly;
    private final Trip trip;
    private final IdFor<Station> towardsStationId;

    private MinuteState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships, GraphNode node,
                        final IdFor<Trip> tripId, IdFor<Station> towardsStationId, final Duration cost,
                        final Towards<MinuteState> builder) {
        super(parent, relationships, cost, builder.getDestination(), node);
        this.trip = super.getTrip(tripId);
        this.towardsStationId = towardsStationId;
    }

    private static IdFor<Trip> getTrip(final GraphNode endNode) {
        if (!endNode.hasTripId()) {
            return new InvalidId<>(Trip.class);
        }
        return endNode.getTripId();
    }

    public IdFor<Service> getServiceId() {
        return trip.getService().getId();
    }

    @Override
    public IdFor<Station> getTowards() {
        return towardsStationId;
    }

    @Override
    protected RouteStationStateOnTrip toRouteStationOnTrip(final RouteStationStateOnTrip.Builder towardsRouteStation, JourneyStateUpdate journeyState,
                                                           final GraphNode routeStationNode, final Duration cost, final boolean isInterchange) {

        return towardsRouteStation.fromMinuteState(journeyState, this, routeStationNode, cost, isInterchange, trip, txn);
    }

    @Override
    protected RouteStationStateEndTrip toRouteStationEndTrip(final RouteStationStateEndTrip.Builder towardsEndTrip,
                                                             JourneyStateUpdate journeyState, final GraphNode routeStationNode,
                                                             final Duration cost, final boolean isInterchange) {

        return towardsEndTrip.fromMinuteState(journeyState, this, routeStationNode, cost, isInterchange, trip, txn);
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                ", trip='" + trip.getId() + '\'' +
                "} " + super.toString();
    }

}
