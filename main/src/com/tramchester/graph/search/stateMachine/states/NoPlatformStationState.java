package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class NoPlatformStationState extends StationState {

    public static class Builder extends StationStateBuilder implements TowardsStation<NoPlatformStationState> {

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
                                                  final GraphTransaction txn) {
            return new NoPlatformStationState(walkingState,
                    boardRelationshipsPlus(node, txn, GROUPED_TO_PARENT, NEIGHBOUR),
                    cost, node, journeyState, this);
        }

        @Override
        public NoPlatformStationState fromStart(final NotStartedState notStartedState, final GraphNode node, final Duration cost,
                                                final JourneyStateUpdate journeyState, final boolean alreadyOnDiversion,
                                                final boolean onDiversion, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> neighbours = getRelationships(txn, node, OUTGOING, NEIGHBOUR);
            final Stream<ImmutableGraphRelationship> initial = boardRelationshipsPlus(node, txn, WALKS_FROM_STATION, GROUPED_TO_PARENT);
            Stream<ImmutableGraphRelationship> relationships = addValidDiversions(node, initial, notStartedState, alreadyOnDiversion, txn);

            return new NoPlatformStationState(notStartedState, Stream.concat(neighbours, relationships), cost, node, journeyState, this);
        }

        public TraversalState fromRouteStation(final RouteStationStateEndTrip routeStationState, final GraphNode node, final Duration cost,
                                               final JourneyStateUpdate journeyState, final boolean alreadyOnDiversion, final GraphTransaction txn) {
            // end of a trip, may need to go back to this route station to catch new service
            final Stream<ImmutableGraphRelationship> initial = boardRelationshipsPlus(node, txn, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
            final Stream<ImmutableGraphRelationship> relationships = addValidDiversions(node, initial, routeStationState, alreadyOnDiversion, txn);
            return new NoPlatformStationState(routeStationState, relationships, cost, node, journeyState, this);
        }

        public TraversalState fromRouteStation(final RouteStationStateOnTrip onTrip, final GraphNode node, final Duration cost,
                                               final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            final Stream<ImmutableGraphRelationship> relationships = boardRelationshipsPlus(node, txn, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
            final Stream<ImmutableGraphRelationship> stationRelationships = filterExcludingEndNode(txn, relationships, onTrip);
            return new NoPlatformStationState(onTrip, stationRelationships, cost, node, journeyState, this);
        }

        @Override
        public NoPlatformStationState fromNeighbour(final StationState noPlatformStation, final GraphNode node, final Duration cost, final JourneyStateUpdate journeyState,
                                                    final boolean onDiversion, final GraphTransaction txn) {
            return new NoPlatformStationState(noPlatformStation,
                    node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT),
                    cost, node, journeyState, this);
        }

        @Override
        public NoPlatformStationState fromGrouped(final GroupedStationState groupedStationState, final GraphNode node, final Duration cost,
                                                  final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            return new NoPlatformStationState(groupedStationState,
                    node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD, NEIGHBOUR),
                    cost,  node, journeyState, this);
        }

        Stream<ImmutableGraphRelationship> boardRelationshipsPlus(final GraphNode node, final GraphTransaction txn, final TransportRelationshipTypes... others) {
            final Stream<ImmutableGraphRelationship> other = node.getRelationships(txn, OUTGOING, others);
            final Stream<ImmutableGraphRelationship> board = node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD);
            // order matters here, i.e. explore walks first
            return Stream.concat(other, board);
        }

    }

    private NoPlatformStationState(TraversalState parent, Stream<ImmutableGraphRelationship> relationships, Duration cost, GraphNode stationNode,
                                   JourneyStateUpdate journeyState, TowardsStation<?> builder) {
        super(parent, relationships, cost, stationNode, journeyState, builder);
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode next, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost, journeyState, onDiversion, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(Builder towardsStation, GraphNode next, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, next, cost);
        return towardsStation.fromNeighbour(this, next, cost, journeyState, onDiversion, txn);
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, GraphNode walkingNode, Duration cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, walkingNode, cost, txn);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, GraphNode groupNode, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, groupNode, cost, txn);
    }

    @Override
    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, GraphNode boardNode, Duration cost,
                                             JourneyStateUpdate journeyState) {
        boardVehicle(boardNode, journeyState);
        return towardsJustBoarded.fromNoPlatformStation(this, boardNode, cost, txn);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode destNode, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost);
    }

    private void boardVehicle(GraphNode node, JourneyStateUpdate journeyState) {
        try {
            TransportMode actualMode = node.getTransportMode();
            journeyState.board(actualMode, node, false);
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
