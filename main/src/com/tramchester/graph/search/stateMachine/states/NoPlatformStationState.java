package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationshipNeo4J;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;

public class NoPlatformStationState extends StationState {

    public static class Builder extends StateBuilder<NoPlatformStationState> implements TowardsStation<NoPlatformStationState>, FromRouteStationStates {

        private final FindStateAfterRouteStation findStateAfterRouteStation;

        public Builder(StateBuilderParameters builderParameters, FindStateAfterRouteStation findStateAfterRouteStation) {
            super(builderParameters);
            this.findStateAfterRouteStation = findStateAfterRouteStation;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.WalkingState, this);
            registers.add(TraversalStateType.NotStartedState, this);
            registers.add(TraversalStateType.RouteStationStateOnTrip, this);
            registers.add(TraversalStateType.RouteStationStateEndTrip, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.GroupedStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.NoPlatformStationState;
        }

        @Override
        public NoPlatformStationState fromWalking(final WalkingState walkingState, final GraphNode node, final Duration cost, final JourneyStateUpdate journeyState,
                                                  final GraphTransactionNeo4J txn) {
            return new NoPlatformStationState(walkingState,
                    boardRelationshipsPlus(node, txn, GROUPED_TO_PARENT, NEIGHBOUR),
                    cost, node, journeyState, getDestination());
        }

        @Override
        public NoPlatformStationState fromStart(final NotStartedState notStartedState, final GraphNode node, final Duration cost,
                                                final JourneyStateUpdate journeyState,
                                                final GraphTransactionNeo4J txn) {

            final Stream<ImmutableGraphRelationshipNeo4J> walksAndGroup = boardRelationshipsPlus(node, txn, WALKS_FROM_STATION, GROUPED_TO_PARENT, NEIGHBOUR);

            final Stream<ImmutableGraphRelationshipNeo4J> relationships = addValidDiversions(walksAndGroup, node, journeyState, txn);

            return new NoPlatformStationState(notStartedState, relationships, cost, node,
                    journeyState, getDestination());
        }

        public TraversalState fromRouteStationEndTrip(final RouteStationStateEndTrip routeStationState, final GraphNode node, final Duration cost,
                                               final JourneyStateUpdate journeyState, final GraphTransactionNeo4J txn) {
            return findStateAfterRouteStation.endTripTowardsStation(getDestination(), routeStationState, node, cost,
                    journeyState, txn, this);
        }

        public TraversalState fromRouteStationOnTrip(final RouteStationStateOnTrip onTrip, final GraphNode node, final Duration cost,
                                               final JourneyStateUpdate journeyState, final GraphTransactionNeo4J txn) {
            return findStateAfterRouteStation.onTripTowardsStation(getDestination(), onTrip, node, cost, journeyState, txn, this);
        }

        @Override
        public NoPlatformStationState fromNeighbour(final StationState noPlatformStation, final GraphNode node, final Duration cost,
                                                    final JourneyStateUpdate journeyState,
                                                    final GraphTransactionNeo4J txn) {
            final Stream<ImmutableGraphRelationshipNeo4J> grouped = node.getRelationships(txn, GraphDirection.Outgoing,GROUPED_TO_PARENT);
            final Stream<ImmutableGraphRelationshipNeo4J> boarding = findStateAfterRouteStation.getBoardingRelationships(txn, node);
            return new NoPlatformStationState(noPlatformStation, Stream.concat(grouped, boarding), cost, node, journeyState, getDestination());
        }

        @Override
        public NoPlatformStationState fromGrouped(final GroupedStationState groupedStationState, final GraphNode node, final Duration cost,
                                                  final JourneyStateUpdate journeyState, final GraphTransactionNeo4J txn) {
            final Stream<ImmutableGraphRelationshipNeo4J> neighbour = node.getRelationships(txn, GraphDirection.Outgoing, BOARD, INTERCHANGE_BOARD, NEIGHBOUR);
            final Stream<ImmutableGraphRelationshipNeo4J> boarding = findStateAfterRouteStation.getBoardingRelationships(txn, node);
            return new NoPlatformStationState(groupedStationState, Stream.concat(neighbour, boarding), cost,  node, journeyState, getDestination());
        }

        Stream<ImmutableGraphRelationshipNeo4J> boardRelationshipsPlus(final GraphNode node, final GraphTransactionNeo4J txn, final TransportRelationshipTypes... others) {
            final Stream<ImmutableGraphRelationshipNeo4J> other = node.getRelationships(txn, GraphDirection.Outgoing, others);
            final Stream<ImmutableGraphRelationshipNeo4J> board = node.getRelationships(txn, GraphDirection.Outgoing, BOARD, INTERCHANGE_BOARD);
            // order matters here, i.e. explore walks first
            return Stream.concat(other, board);
        }

    }

    NoPlatformStationState(final ImmutableTraversalState parent, final Stream<ImmutableGraphRelationshipNeo4J> relationships, final Duration cost,
                           final GraphNode stationNode, final JourneyStateUpdate journeyStateUpdate, final TraversalStateType builderStateTYpe) {
        super(parent, relationships, cost, stationNode, journeyStateUpdate, builderStateTYpe);
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode next, final Duration cost,
                                                     final JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(final Builder towardsStation, final GraphNode next, final Duration cost,
                                                 final JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toWalk(final WalkingState.Builder towardsWalk, final GraphNode walkingNode,
                                    final Duration cost, final JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, walkingNode, cost, txn);
    }

    @Override
    protected TraversalState toGrouped(final GroupedStationState.Builder towardsGroup, JourneyStateUpdate journeyStateUpdate, final GraphNode groupNode,
                                       final Duration cost, final JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, journeyStateUpdate, groupNode, cost, txn);
    }

    @Override
    protected JustBoardedState toJustBoarded(final JustBoardedState.Builder towardsJustBoarded, final GraphNode boardNode,
                                             final Duration cost, final JourneyStateUpdate journeyState) {
        boardVehicle(boardNode, journeyState);
        return towardsJustBoarded.fromNoPlatformStation(journeyState, this, boardNode, cost, txn);
    }

    @Override
    protected void toDestination(final DestinationState.Builder towardsDestination, final GraphNode boardingNode,
                                 final Duration cost, final JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost, boardingNode);
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

    @Override
    public GraphNodeId nodeId() {
        return stationNode.getId();
    }

}
