package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformStationState extends StationState {

    public static class Builder extends StateBuilder<PlatformStationState> implements TowardsStation<PlatformStationState>  {

        protected Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
        }

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

        @Override
        public PlatformStationState fromWalking(final WalkingState walkingState, final GraphNode stationNode, final Duration cost,
                                                final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = stationNode.getRelationships(txn, OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT,
                    NEIGHBOUR);
            return new PlatformStationState(walkingState, relationships, cost, stationNode, journeyState, this);
        }

        public PlatformStationState fromPlatform(final PlatformState platformState, final GraphNode stationNode, final Duration cost,
                                                 final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> initial = stationNode.getRelationships(txn, OUTGOING, WALKS_FROM_STATION, ENTER_PLATFORM,
                    NEIGHBOUR, GROUPED_TO_PARENT);
            final Stream<ImmutableGraphRelationship> diversions = addValidDiversions(stationNode, journeyState, txn);
            final Stream<ImmutableGraphRelationship> relationships = Stream.concat(initial, diversions);
            return new PlatformStationState(platformState, filterExcludingEndNode(txn, relationships, platformState), cost,
                    stationNode, journeyState, this);
        }

        public PlatformStationState fromStart(final NotStartedState notStartedState, final GraphNode stationNode, final Duration cost,
                                              final JourneyStateUpdate journeyState,
                                              final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> initial = stationNode.getRelationships(txn, OUTGOING, WALKS_FROM_STATION,
                    GROUPED_TO_PARENT, ENTER_PLATFORM, NEIGHBOUR);
            final Stream<ImmutableGraphRelationship> diversions = addValidDiversions(stationNode, journeyState, txn);

            final Stream<ImmutableGraphRelationship> relationships = Stream.concat(initial, diversions);
            return new PlatformStationState(notStartedState, relationships, cost, stationNode, journeyState, this);
        }

        @Override
        public PlatformStationState fromNeighbour(final StationState stationState, final GraphNode stationNode, final Duration cost,
                                                  final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> initial = stationNode.getRelationships(txn, OUTGOING, ENTER_PLATFORM, GROUPED_TO_PARENT);
            final Stream<ImmutableGraphRelationship> diversions = addValidDiversions(stationNode, journeyState, txn);

            final Stream<ImmutableGraphRelationship> relationships = Stream.concat(initial, diversions);
            return new PlatformStationState(stationState, relationships, cost, stationNode, journeyState, this);
        }

        public PlatformStationState fromGrouped(final GroupedStationState groupedStationState, final GraphNode stationNode, final Duration cost,
                                                final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = stationNode.getRelationships(txn, OUTGOING, ENTER_PLATFORM, NEIGHBOUR);
            return new PlatformStationState(groupedStationState, relationships, cost, stationNode, journeyState, this);
        }

    }

    private PlatformStationState(final ImmutableTraversalState parent, final Stream<ImmutableGraphRelationship> relationships,
                                 final Duration cost, final GraphNode stationNode,
                                 final JourneyStateUpdate journeyState, final TowardsStation<?> builder) {
        super(parent, relationships, cost, stationNode, journeyState, builder.getDestination());
    }

    @Override
    public String toString() {
        return "PlatformStationState{" +
                "stationNodeId=" + stationNode.getId() +
                "} " + super.toString();
    }


    @Override
    public GraphNodeId nodeId() {
        return stationNode.getId();
    }

    @Override
    protected TraversalState toWalk(final WalkingState.Builder towardsWalk, final GraphNode node, final Duration cost, final JourneyStateUpdate journeyState) {
        journeyState.beginWalk(stationNode, false, cost);
        return towardsWalk.fromStation(this, node, cost, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder toStation, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, node, cost);
        return toStation.fromNeighbour(this, node, cost, journeyState, txn);
    }

    @Override
    protected PlatformStationState toPlatformStation(final Builder towardsStation, final GraphNode node, final Duration cost,
                                                     final JourneyStateUpdate journeyState) {
        journeyState.toNeighbour(stationNode, node, cost);
        return towardsStation.fromNeighbour(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toGrouped(final GroupedStationState.Builder towardsGroup, JourneyStateUpdate journeyStateUpdate, final GraphNode node, final Duration cost,
                                       final JourneyStateUpdate journeyState) {
        return towardsGroup.fromChildStation(this, journeyStateUpdate, node, cost, txn);
    }

    @Override
    protected TraversalState toPlatform(final PlatformState.Builder towardsPlatform, final GraphNode node,
                                        final Duration cost, final JourneyStateUpdate journeyState) {
        return towardsPlatform.from(this, node, cost, txn);
    }

    @Override
    protected void toDestination(final DestinationState.Builder towardsDestination, final GraphNode node, final Duration cost, final JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost, node);
    }
}
