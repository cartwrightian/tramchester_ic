package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
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

        public TraversalState fromChildStation(StationState stationState, GraphNode node, Duration cost) {
            return new GroupedStationState(stationState,
                    filterExcludingEndNode(node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),stationState),
                    cost, node.getIdOLD(), this);
        }

        public TraversalState fromStart(NotStartedState notStartedState, GraphNode node, Duration cost) {
            return new GroupedStationState(notStartedState, node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),
                    cost, node.getIdOLD(), this);
        }
    }

    private final long stationNodeId;

    private GroupedStationState(TraversalState parent, Stream<GraphRelationship> relationships, Duration cost, long stationNodeId, Towards<GroupedStationState> builder) {
        super(parent, relationships, cost, builder.getDestination());
        this.stationNodeId = stationNodeId;
    }

//    private GroupedStationState(TraversalState parent, ResourceIterable<Relationship> relationships, Duration cost, long stationNodeId, Towards<GroupedStationState> builder) {
//        super(parent, relationships, cost, builder.getDestination());
//        this.stationNodeId = stationNodeId;
//    }

    @Override
    public String toString() {
        return "GroupedStationState{" +
                "stationNodeId=" + stationNodeId +
                "} " + super.toString();
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromGrouped(this, node, cost, journeyState);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromGrouped(this, node, cost, journeyState);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        towardsDestination.from(this, cost);
    }

}
