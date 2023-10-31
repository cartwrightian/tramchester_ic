package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformStationState extends StationState {

    public static class Builder extends StationStateBuilder implements TowardsStation<PlatformStationState>  {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.WalkingState, this);
            registers.add(TraversalStateType.PlatformState, this);
            registers.add(TraversalStateType.NotStartedState, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.GroupedStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.PlatformStationState;
        }

        public PlatformStationState fromWalking(WalkingState walkingState, GraphNode stationNode, Duration cost, JourneyStateUpdate journeyState) {
            final Stream<GraphRelationship> relationships = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT,
                    NEIGHBOUR);
            return new PlatformStationState(walkingState, relationships, cost, stationNode, journeyState, this);
        }

        public PlatformStationState fromPlatform(PlatformState platformState, GraphNode stationNode, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
            final Stream<GraphRelationship> initial = stationNode.getRelationships(OUTGOING, WALKS_FROM_STATION, ENTER_PLATFORM,
                    NEIGHBOUR, GROUPED_TO_PARENT);
            Stream<GraphRelationship> relationships = addValidDiversions(stationNode, initial, platformState, onDiversion);
            return new PlatformStationState(platformState, filterExcludingEndNode(relationships, platformState), cost,
                    stationNode, journeyState, this);
        }

        public PlatformStationState fromStart(NotStartedState notStartedState, GraphNode stationNode, Duration cost,
                                              JourneyStateUpdate journeyState, boolean alreadyOnDiversion, boolean onDiversion) {
            final Stream<GraphRelationship> neighbours = TraversalState.getRelationships(stationNode, OUTGOING, NEIGHBOUR);
            final Stream<GraphRelationship> initial = stationNode.getRelationships(OUTGOING, WALKS_FROM_STATION,
                    GROUPED_TO_PARENT, ENTER_PLATFORM);
            Stream<GraphRelationship> relationships = addValidDiversions(stationNode, initial, notStartedState, onDiversion);

            return new PlatformStationState(notStartedState, Stream.concat(neighbours,relationships), cost, stationNode, journeyState, this);
        }

        @Override
        public PlatformStationState fromNeighbour(StationState stationState, GraphNode stationNode, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
            final Stream<GraphRelationship> initial = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT);
            Stream<GraphRelationship> relationships = addValidDiversions(stationNode, initial, stationState, onDiversion);
            return new PlatformStationState(stationState, relationships, cost, stationNode, journeyState, this);
        }

        public PlatformStationState fromGrouped(GroupedStationState groupedStationState, GraphNode stationNode, Duration cost,
                                                JourneyStateUpdate journeyState) {
            final Stream<GraphRelationship> relationships = stationNode.getRelationships(OUTGOING, ENTER_PLATFORM, NEIGHBOUR);
            return new PlatformStationState(groupedStationState, relationships, cost, stationNode, journeyState, this);
        }

    }

    private PlatformStationState(TraversalState parent, Stream<GraphRelationship> relationships, Duration cost, GraphNode stationNode,
                                 JourneyStateUpdate journeyState, TowardsStation<?> builder) {
        super(parent, relationships, cost, stationNode, journeyState, builder);
    }

//    private PlatformStationState(TraversalState parent, ResourceIterable<Relationship> relationships, Duration cost, Node stationNode,
//                                 JourneyStateUpdate journeyState, TowardsStation<?> builder) {
//        super(parent, relationships, cost, stationNode, journeyState, builder);
//    }

    @Override
    public String toString() {
        return "PlatformStationState{" +
                "stationNodeId=" + stationNode.getId() +
                "} " + super.toString();
    }


    @Override
    public long nodeId() {
        return stationNode.getId();
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder toStation, GraphNode node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, node, cost);
        return toStation.fromNeighbour(this, node, cost, journeyState, onDiversion);
    }

    @Override
    protected PlatformStationState toPlatformStation(Builder towardsStation, GraphNode node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.toNeighbour(stationNode, node, cost);
        return towardsStation.fromNeighbour(this, node, cost, journeyState, onDiversion);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, node, cost);
    }

    @Override
    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsPlatform.from(this, node, cost);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost);
    }
}
