package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphProps;
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
        public NoPlatformStationState fromWalking(WalkingState walkingState, GraphNode node, Duration cost, JourneyStateUpdate journeyState, GraphTransaction txn) {
            return new NoPlatformStationState(walkingState,
                    boardRelationshipsPlus(node, txn, GROUPED_TO_PARENT, NEIGHBOUR),
                    cost, node, journeyState, this);
        }

        @Override
        public NoPlatformStationState fromStart(NotStartedState notStartedState, GraphNode node, Duration cost,
                                                JourneyStateUpdate journeyState, boolean alreadyOnDiversion, boolean onDiversion, GraphTransaction txn) {
            final Stream<GraphRelationship> neighbours = getRelationships(txn, node, OUTGOING, NEIGHBOUR);
            final Stream<GraphRelationship> initial = boardRelationshipsPlus(node, txn, WALKS_FROM_STATION, GROUPED_TO_PARENT);
            Stream<GraphRelationship> relationships = addValidDiversions(node, initial, notStartedState, alreadyOnDiversion, txn);

            return new NoPlatformStationState(notStartedState, Stream.concat(neighbours, relationships), cost, node, journeyState, this);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, GraphNode node, Duration cost,
                                               JourneyStateUpdate journeyState, boolean alreadyOnDiversion, GraphTransaction txn) {
            // end of a trip, may need to go back to this route station to catch new service
            final Stream<GraphRelationship> initial = boardRelationshipsPlus(node, txn, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<GraphRelationship> relationships = addValidDiversions(node, initial, routeStationState, alreadyOnDiversion, txn);
            return new NoPlatformStationState(routeStationState, relationships, cost, node, journeyState, this);
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip onTrip, GraphNode node, Duration cost, JourneyStateUpdate journeyState, GraphTransaction txn) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            final Stream<GraphRelationship> relationships = boardRelationshipsPlus(node, txn, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<GraphRelationship> stationRelationships = filterExcludingEndNode(txn, relationships, onTrip);
            return new NoPlatformStationState(onTrip, stationRelationships, cost, node, journeyState, this);
        }

        @Override
        public NoPlatformStationState fromNeighbour(StationState noPlatformStation, GraphNode node, Duration cost, JourneyStateUpdate journeyState,
                                                    boolean onDiversion, GraphTransaction txn) {
            return new NoPlatformStationState(noPlatformStation,
                    node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT),
                    cost, node, journeyState, this);
        }

        @Override
        public NoPlatformStationState fromGrouped(GroupedStationState groupedStationState, GraphNode node, Duration cost, JourneyStateUpdate journeyState, GraphTransaction txn) {
            return new NoPlatformStationState(groupedStationState,
                    node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD, NEIGHBOUR),
                    cost,  node, journeyState, this);
        }

        Stream<GraphRelationship> boardRelationshipsPlus(GraphNode node, GraphTransaction txn, TransportRelationshipTypes... others) {
            Stream<GraphRelationship> other = node.getRelationships(txn, OUTGOING, others);
            Stream<GraphRelationship> board = node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD);
            // order matters here, i.e. explore walks first
            return Stream.concat(other, board);
        }

    }

    private NoPlatformStationState(TraversalState parent, Stream<GraphRelationship> relationships, Duration cost, GraphNode stationNode,
                                   JourneyStateUpdate journeyState, TowardsStation<?> builder) {
        super(parent, relationships, cost, stationNode, journeyState, builder);
    }

//    private NoPlatformStationState(TraversalState parent, ResourceIterable<Relationship> relationships, Duration cost, Node stationNode,
//                                   JourneyStateUpdate journeyState, TowardsStation<?> builder) {
//        super(parent, relationships, cost, stationNode, journeyState, builder);
//    }

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
    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, GraphNode boardNode, Duration cost, JourneyStateUpdate journeyState) {
        boardVehicle(boardNode, journeyState);
        return towardsJustBoarded.fromNoPlatformStation(this, boardNode, cost, txn);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode destNode, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost);
    }

    private void boardVehicle(GraphNode node, JourneyStateUpdate journeyState) {
        try {
            TransportMode actualMode = GraphProps.getTransportMode(node);
            journeyState.board(actualMode, node, false);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board vehicle", e);
        }
    }

    @Override
    public String toString() {
        return "NoPlatformStationState{" +
                "stationNodeId=" + stationNode.getIdOLD() +
                "} " + super.toString();
    }

    @Override
    public long nodeId() {
        return stationNode.getIdOLD();
    }

}
