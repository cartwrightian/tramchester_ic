package com.tramchester.graph.search.stateMachine.states;


import com.tramchester.graph.facade.*;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_GROUPED;

public class GroupedStationState extends TraversalState {

    public static class Builder implements Towards<GroupedStationState> {

        // TODO map of accept states to outbound relationships

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.NotStartedState, this);
            registers.add(TraversalStateType.GroupedStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.GroupedStationState;
        }

        public TraversalState fromChildStation(StationState stationState, GraphNode node, Duration cost, GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = filterExcludingEndNode(txn,
                    node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD, GROUPED_TO_GROUPED), stationState);
            return new GroupedStationState(stationState, relationships, cost, this, node);
        }

        public TraversalState fromStart(NotStartedState notStartedState, GraphNode node, Duration cost, GraphTransaction txn) {
            return new GroupedStationState(notStartedState, node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD, GROUPED_TO_GROUPED),
                    cost, this, node);
        }

        public TraversalState fromGrouped(GroupedStationState parent, Duration cost, GraphNode node, GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = filterExcludingEndNode(txn,
                    node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD, GROUPED_TO_GROUPED), parent);
            return new GroupedStationState(parent, relationships, cost, this, node);
        }
    }

    private GroupedStationState(TraversalState parent, Stream<ImmutableGraphRelationship> relationships, Duration cost,
                                Towards<GroupedStationState> builder, GraphNode graphNode) {
        super(parent, relationships, cost, builder.getDestination(), graphNode);
    }

    @Override
    public String toString() {
        return "GroupedStationState{" +
                "} " + super.toString();
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromGrouped(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromGrouped(this, node, cost, journeyState, txn);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost, node);
    }

    @Override
    protected TraversalState toGrouped(Builder towardsGroup, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromGrouped(this, cost, node, txn);
    }
}
