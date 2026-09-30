package com.tramchester.graph.search.stateMachine.states.station;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.stateMachine.journeyState.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.journeyState.TraversalStateType;
import com.tramchester.graph.search.stateMachine.states.*;
import com.tramchester.graph.search.stateMachine.states.routeStation.JustBoardedState;
import com.tramchester.graph.search.stateMachine.states.routeStation.RouteStationStateEndTrip;
import com.tramchester.graph.search.stateMachine.states.routeStation.RouteStationStateOnTrip;

import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.*;

public class NoPlatformStationState extends StationState {

    public static class Builder extends StationBuilder<NoPlatformStationState> implements FromRouteStationStates {

        private final FindStateAfterRouteStation findStateAfterRouteStation;

        public Builder(StateBuilderParameters builderParameters, FindStateAfterRouteStation findStateAfterRouteStation) {
            super(builderParameters);
            this.findStateAfterRouteStation = findStateAfterRouteStation;
        }

        @Override
        public void register(RegistersFromState registers) {
            super.register(registers);
            registers.add(TraversalStateType.RouteStationStateOnTrip, this);
            registers.add(TraversalStateType.RouteStationStateEndTrip, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.NoPlatformStationState;
        }

        @Override
        public NoPlatformStationState fromWalking(final WalkingState walkingState, final GraphNode node, final TramDuration cost, final JourneyStateUpdate journeyState,
                                                  final GraphTransaction txn) {
            final ImmutableEnumSet<TransportRelationshipTypes> fromWalking = ImmutableEnumSet.of(GROUPED_TO_PARENT, NEIGHBOUR);
            return new NoPlatformStationState(walkingState, boardRelationshipsPlus(node, txn, fromWalking),
                    cost, node, journeyState, getDestination());
        }

        @Override
        public NoPlatformStationState fromStart(final NotStartedState notStartedState, final GraphNode node, final TramDuration cost,
                                                final JourneyStateUpdate journeyState,
                                                boolean arrivedViaDiversion,
                                                final GraphTransaction txn) {

            final ImmutableEnumSet<TransportRelationshipTypes> fromStart = ImmutableEnumSet.of(WALKS_FROM_STATION, GROUPED_TO_PARENT, NEIGHBOUR);

            final Stream<GraphRelationship> walksAndGroup = boardRelationshipsPlus(node, txn, fromStart);
            final Stream<GraphRelationship> relationships = addValidDiversions(walksAndGroup, node, arrivedViaDiversion, txn);

            return new NoPlatformStationState(notStartedState, relationships, cost, node,
                    journeyState, getDestination());
        }

        @Override
        public TraversalState fromRouteStationEndTrip(final RouteStationStateEndTrip routeStationState, final GraphNode node,
                                                      final TramDuration cost, final JourneyStateUpdate journeyState,
                                                      final GraphTransaction txn) {
            return findStateAfterRouteStation.endTripTowardsStation(getDestination(), routeStationState, node, cost,
                    journeyState, txn, this);
        }

        @Override
        public TraversalState fromRouteStationOnTrip(final RouteStationStateOnTrip onTrip, final GraphNode node, final TramDuration cost,
                                                     final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            return findStateAfterRouteStation.onTripTowardsStation(getDestination(), onTrip, node, cost, journeyState, txn, this);
        }

        @Override
        public NoPlatformStationState fromNeighbour(final StationState noPlatformStation, final GraphNode node, final TramDuration cost,
                                                    final JourneyStateUpdate journeyState,
                                                    final GraphTransaction txn, boolean viaDivert) {
            final Stream<GraphRelationship> grouped = node.getRelationships(txn, GraphDirection.Outgoing, GROUPED_TO_PARENT);
            final Stream<GraphRelationship> boarding = findStateAfterRouteStation.getBoardingRelationships(txn, node);
            // TODO MISSING DIVERSIONS HERE??
            return new NoPlatformStationState(noPlatformStation, Stream.concat(grouped, boarding), cost, node, journeyState,
                    getDestination());
        }

        @Override
        public NoPlatformStationState fromGrouped(final GroupedStationState groupedStationState, final GraphNode node, final TramDuration cost,
                                                  final JourneyStateUpdate journeyState,
                                                  final GraphTransaction txn, final boolean viaDivert) {
            // TODO MISSING DIVERSIONS HERE??
            final ImmutableEnumSet<TransportRelationshipTypes> fromGrouped = ImmutableEnumSet.of(BOARD, INTERCHANGE_BOARD, NEIGHBOUR);
            final Stream<GraphRelationship> neighbour = node.getRelationships(txn, GraphDirection.Outgoing, fromGrouped);
            final Stream<GraphRelationship> boarding = findStateAfterRouteStation.getBoardingRelationships(txn, node);
            return new NoPlatformStationState(groupedStationState, Stream.concat(neighbour, boarding), cost,  node, journeyState, getDestination());
        }

        Stream<GraphRelationship> boardRelationshipsPlus(final GraphNode node, final GraphTransaction txn,
                                                         final ImmutableEnumSet<TransportRelationshipTypes> others) {
            final ImmutableEnumSet<TransportRelationshipTypes> boards = ImmutableEnumSet.of(BOARD, INTERCHANGE_BOARD);

            final Stream<GraphRelationship> other = node.getRelationships(txn, GraphDirection.Outgoing, others);
            final Stream<GraphRelationship> board = node.getRelationships(txn, GraphDirection.Outgoing, boards);
            // order matters here, i.e. explore walks first
            return Stream.concat(other, board);
        }

    }

    public NoPlatformStationState(final ImmutableTraversalState parent, final Stream<GraphRelationship> relationships, final TramDuration cost,
                                  final GraphNode stationNode, final JourneyStateUpdate journeyStateUpdate, final TraversalStateType builderStateTYpe) {
        super(parent, relationships, cost, stationNode, journeyStateUpdate, builderStateTYpe);
    }

    @Override
    protected JustBoardedState toJustBoarded(final JustBoardedState.Builder towardsJustBoarded, final GraphNode boardNode,
                                             final TramDuration cost, final JourneyStateUpdate journeyState) {
        boardVehicle(boardNode, journeyState);
        return towardsJustBoarded.fromNoPlatformStation(journeyState, this, boardNode, cost, txn);
    }

    private void boardVehicle(final GraphNode boardingNode, final JourneyStateUpdate journeyState) {
        try {
            final TransportMode actualMode = boardingNode.getTransportMode();
            journeyState.board(actualMode, boardingNode, false);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board vehicle", e);
        }
    }

    @Override
    public String toString() {
        return "NoPlatformStationState{" +
                "stationNodeId=" + stationNode.getId() +
                "} " + super.toString();
    }

}
