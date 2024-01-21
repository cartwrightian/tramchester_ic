package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.NodeContentsRepository;
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

public class MinuteState extends TraversalState {

    public static class Builder implements Towards<MinuteState> {

        private final boolean changeAtInterchangeOnly;
        private final NodeContentsRepository nodeContents;

        public Builder(boolean changeAtInterchangeOnly, NodeContentsRepository nodeContents) {
            this.changeAtInterchangeOnly = changeAtInterchangeOnly;
            this.nodeContents = nodeContents;
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
                                       final JourneyStateUpdate journeyState, final TransportRelationshipTypes[] currentModes, final GraphTransaction txn) {

            final Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, OUTGOING, currentModes);

            if (existingTrip.isOnTrip()) {
                final IdFor<Trip> existingTripId = existingTrip.getTripId();
                final Stream<ImmutableGraphRelationship> filterBySingleTripId = filterBySingleTripId(relationships, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, existingTripId, cost, changeAtInterchangeOnly, this);
            } else {
                // starting a brand-new journey, since at minute node now have specific tripid to use
                final IdFor<Trip> newTripId = getTrip(node);
                journeyState.beginTrip(newTripId);
                return new MinuteState(hourState, relationships, newTripId, cost, changeAtInterchangeOnly, this);
            }
        }

        private Stream<ImmutableGraphRelationship> filterBySingleTripId(final Stream<ImmutableGraphRelationship> relationships, final IdFor<Trip> existingTripId) {
            return relationships.filter(relationship -> nodeContents.getTripId(relationship).equals(existingTripId));
        }
    }

    private final boolean interchangesOnly;
    private final Trip trip;

    private MinuteState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships, final IdFor<Trip> tripId, final Duration cost,
                        final boolean interchangesOnly, final Towards<MinuteState> builder) {
        super(parent, relationships, cost, builder.getDestination());
        this.trip = traversalOps.getTrip(tripId);
        this.interchangesOnly = interchangesOnly;
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
    protected RouteStationStateOnTrip toRouteStationOnTrip(final RouteStationStateOnTrip.Builder towardsRouteStation,
                                                           final GraphNode routeStationNode, final Duration cost, final boolean isInterchange) {

        return towardsRouteStation.fromMinuteState(this, routeStationNode, cost, isInterchange, trip, txn);
    }

    @Override
    protected RouteStationStateEndTrip toRouteStationEndTrip(final RouteStationStateEndTrip.Builder towardsEndTrip,
                                                             final GraphNode routeStationNode,
                                                             final Duration cost, final boolean isInterchange) {

        return towardsEndTrip.fromMinuteState(this, routeStationNode, cost, isInterchange, trip, txn);
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "interchangesOnly=" + interchangesOnly +
                ", trip='" + trip.getId() + '\'' +
                "} " + super.toString();
    }

}
