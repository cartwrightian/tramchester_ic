package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.RegistersFromState;

import java.time.Duration;
import java.util.stream.Stream;

import static java.lang.String.format;

public class PlatformState extends TraversalState implements NodeId {

    public static class Builder extends StateBuilder<PlatformState> implements FromRouteStationStates {

        private final FindStateAfterRouteStation findStateAfterRouteStation;

        public Builder(StateBuilderParameters stateBuilderParameters, FindStateAfterRouteStation findStateAfterRouteStation) {
            super(stateBuilderParameters);
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

        public PlatformState from(final PlatformStationState stationState, final GraphNode node, final Duration cost, final GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> boarding = findStateAfterRouteStation.getBoardingRelationships(txn, node);

            return new PlatformState(stationState, boarding, node, cost, this.getDestination());
        }

        public TraversalState fromRouteStationOnTrip(final RouteStationStateOnTrip routeStationStateOnTrip, final GraphNode node,
                                                     final Duration cost, final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            return findStateAfterRouteStation.onTripTowardsPlatform(getDestination(), routeStationStateOnTrip, node, cost, txn, this);
        }

        public TraversalState fromRouteStationEndTrip(final RouteStationStateEndTrip routeStationState, final GraphNode node,
                                                      final Duration cost,
                                                      JourneyStateUpdate journeyState,
                                                      final GraphTransaction txn) {
            return findStateAfterRouteStation.endTripTowardsPlatform(getDestination(), routeStationState, node, cost, txn, this);
        }

    }

    private final GraphNode platformNode;

    PlatformState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships, final GraphNode platformNode,
                  final Duration cost, final TraversalStateType towards) {
        super(parent, relationships, cost, towards, platformNode);
        this.platformNode = platformNode;
    }

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNode.getId() +
                "} " + super.toString();
    }

    @Override
    protected JustBoardedState toJustBoarded(final JustBoardedState.Builder towardsJustBoarded, final GraphNode boardingNode,
                                             final Duration cost, final JourneyStateUpdate journeyState) {
        try {
            final TransportMode actualMode = boardingNode.getTransportMode();
            if (actualMode==null) {
                throw new RuntimeException(format("Unable get transport mode at %s for %s", boardingNode.getLabels(), boardingNode.getAllProperties()));
            }
            journeyState.board(actualMode, platformNode, true);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board tram", e);
        }
        return towardsJustBoarded.fromPlatformState(journeyState, this, boardingNode, cost, txn);
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                     final JourneyStateUpdate journeyState) {
        return towardsStation.fromPlatform(this, node, cost, journeyState, txn);
    }

    @Override
    public GraphNodeId nodeId() {
        return platformNode.getId();
    }
}
