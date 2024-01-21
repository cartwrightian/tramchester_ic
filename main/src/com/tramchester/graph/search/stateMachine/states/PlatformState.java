package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.*;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformState extends TraversalState implements NodeId {

    public static class Builder implements Towards<PlatformState>, FromRouteStationStates {

        private final FindStateAfterRouteStation findStateAfterRouteStation;

        public Builder(FindStateAfterRouteStation findStateAfterRouteStation) {
            this.findStateAfterRouteStation = findStateAfterRouteStation;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.RouteStationStateOnTrip, this);
            registers.add(TraversalStateType.RouteStationStateEndTrip, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.PlatformState;
        }

        public PlatformState from(final PlatformStationState platformStationState, final GraphNode node, final Duration cost, final GraphTransaction txn) {
            // inc. board here since might be starting journey
            return new PlatformState(platformStationState,
                    node.getRelationships(txn, OUTGOING, INTERCHANGE_BOARD, BOARD), node, cost, this.getDestination());
        }

        public TraversalState fromRouteStationOnTrip(final RouteStationStateOnTrip routeStationStateOnTrip, final GraphNode node,
                                                     final Duration cost, final JourneyStateUpdate journeyState, final GraphTransaction txn) {

            return findStateAfterRouteStation.onTripTowardsPlatform(getDestination(), routeStationStateOnTrip, node, cost, txn);
//            // towards final destination, just follow this one
//            final OptionalResourceIterator<ImmutableGraphRelationship> towardsDest = getTowardsDestination(routeStationStateOnTrip.traversalOps, node, txn);
//            if (!towardsDest.isEmpty()) {
//                return new PlatformState(routeStationStateOnTrip, towardsDest.stream(), node, cost, this);
//            }
//
//            // inc. board here since might be starting journey
//            final Stream<ImmutableGraphRelationship> platformRelationships = node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
//
//            // Cannot filter here as might be starting a new trip from this point, so need to 'go back' to the route station
//            //Stream<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationStateOnTrip);
//            return new PlatformState(routeStationStateOnTrip, platformRelationships, node, cost, this);
        }

        public TraversalState fromRouteStationEndTrip(final RouteStationStateEndTrip routeStationState, final GraphNode node,
                                                      final Duration cost,
                                                      JourneyStateUpdate journeyState, boolean alreadyOnDiversion,
                                                      final GraphTransaction txn) {

            return findStateAfterRouteStation.endTripTowardsPlatform(getDestination(), routeStationState, node, cost, txn);
            // towards final destination, just follow this one
//            final OptionalResourceIterator<ImmutableGraphRelationship> towardsDest = getTowardsDestination(routeStationState.traversalOps, node, txn);
//            if (!towardsDest.isEmpty()) {
//                return new PlatformState(routeStationState, towardsDest.stream(), node, cost, this.getDestination());
//            }
//
//            final Stream<ImmutableGraphRelationship> platformRelationships = node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
//            // end of a trip, may need to go back to this route station to catch new service
//            return new PlatformState(routeStationState, platformRelationships, node, cost, this.getDestination());
        }

//        private OptionalResourceIterator<ImmutableGraphRelationship> getTowardsDestination(TraversalOps traversalOps, GraphNode node, GraphTransaction txn) {
//            return traversalOps.getTowardsDestination(node.getRelationships(txn, OUTGOING, LEAVE_PLATFORM));
//        }

    }

    private final GraphNode platformNode;

    PlatformState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships, final GraphNode platformNode,
                  final Duration cost, final TraversalStateType towards) {
        super(parent, relationships, cost, towards);
        this.platformNode = platformNode;
    }

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNode.getId() +
                "} " + super.toString();
    }

    @Override
    protected JustBoardedState toJustBoarded(final JustBoardedState.Builder towardsJustBoarded, final GraphNode node, final Duration cost,
                                             final JourneyStateUpdate journeyState) {
        try {
            final TransportMode actualMode = node.getTransportMode();
            if (actualMode==null) {
                throw new RuntimeException(format("Unable get transport mode at %s for %s", node.getLabels(), node.getAllProperties()));
            }
            journeyState.board(actualMode, platformNode, true);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board tram", e);
        }
        return towardsJustBoarded.fromPlatformState(this, node, cost, txn);
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                     final JourneyStateUpdate journeyState, final boolean onDiversion) {
        return towardsStation.fromPlatform(this, node, cost, journeyState, onDiversion, txn);
    }

    @Override
    public GraphNodeId nodeId() {
        return platformNode.getId();
    }
}
