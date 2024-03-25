package com.tramchester.graph.search.stateMachine.states;


import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_GROUPED;

public class GroupedStationState extends TraversalState {

    public static class Builder extends StateBuilder<GroupedStationState> {
        protected Builder(StateBuilderParameters parameters) {
            super(parameters);
        }

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

        public TraversalState fromChildStation(StationState stationState, JourneyStateUpdate journeyStateUpdate,
                                               GraphNode node, Duration cost, GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = filterExcludingEndNode(txn,
                    node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD, GROUPED_TO_GROUPED), stationState);
            return new GroupedStationState(stationState, journeyStateUpdate, relationships, cost, this, node);
        }

        public TraversalState fromStart(NotStartedState notStartedState, GraphNode node, JourneyStateUpdate journeyStateUpdate,
                                        Duration cost, GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD, GROUPED_TO_GROUPED);
            return new GroupedStationState(notStartedState, journeyStateUpdate, relationships,
                    cost, this, node);
        }

        public TraversalState fromGrouped(GroupedStationState parent, Duration cost, JourneyStateUpdate journeyStateUpdate,
                                          GraphNode node, GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = filterExcludingEndNode(txn,
                    node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD, GROUPED_TO_GROUPED), parent);
            return new GroupedStationState(parent, journeyStateUpdate, relationships, cost, this, node);
        }
    }

    private GroupedStationState(ImmutableTraversalState parent, final JourneyStateUpdate journeyStateUpdate,
                                Stream<ImmutableGraphRelationship> relationships, Duration cost,
                                Towards<GroupedStationState> builder, GraphNode graphNode) {
        super(parent, relationships, cost, builder.getDestination(), graphNode);
        final IdFor<StationGroup> stationGroupdId = graphNode.getStationGroupId();
        journeyStateUpdate.seenStationGroup(stationGroupdId);
    }

    @Override
    public String toString() {
        return "GroupedStationState{" +
                "} " + super.toString();
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                     JourneyStateUpdate journeyState) {
        return towardsStation.fromGrouped(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                 JourneyStateUpdate journeyState) {
        return towardsStation.fromGrouped(this, node, cost, journeyState, txn);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost, node);
    }

    @Override
    protected TraversalState toGrouped(Builder towardsGroup, JourneyStateUpdate journeyStateUpdate, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        return towardsGroup.fromGrouped(this, cost, journeyStateUpdate, node, txn);
    }
}
