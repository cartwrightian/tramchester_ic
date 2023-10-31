package com.tramchester.graph.search.stateMachine.states;


import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;

public class GroupedStationState extends TraversalState {

    public static class Builder implements Towards<GroupedStationState> {

        // TODO map of accept states to outbound relationships

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.NotStartedState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.GroupedStationState;
        }

        public TraversalState fromChildStation(StationState stationState, GraphNode node, Duration cost, GraphTransaction txn) {
            return new GroupedStationState(stationState,
                    filterExcludingEndNode(txn, node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD),stationState),
                    cost, node.getId(), this);
        }

        public TraversalState fromStart(NotStartedState notStartedState, GraphNode node, Duration cost, GraphTransaction txn) {
            return new GroupedStationState(notStartedState, node.getRelationships(txn, Direction.OUTGOING, GROUPED_TO_CHILD),
                    cost, node.getId(), this);
        }
    }

    private final GraphNodeId stationNodeId;

    private GroupedStationState(TraversalState parent, Stream<GraphRelationship> relationships, Duration cost, GraphNodeId stationNodeId,
                                Towards<GroupedStationState> builder) {
        super(parent, relationships, cost, builder.getDestination());
        this.stationNodeId = stationNodeId;
    }

    @Override
    public String toString() {
        return "GroupedStationState{" +
                "stationNodeId=" + stationNodeId +
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
        towardsDestination.from(this, cost);
    }

}
