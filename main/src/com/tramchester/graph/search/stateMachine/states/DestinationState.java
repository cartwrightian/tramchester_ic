package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.stream.Stream;

/**
 * Used for the path to state mapping
 * The traversal code will stop before reaching here as it checks the destination node id's before invoking the next
 * state.
 */
public class DestinationState extends TraversalState
{

    public static class Builder extends StateBuilder<DestinationState> {

        protected Builder(StateBuilderParameters parameters) {
            super(parameters);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.WalkingState, this);
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.GroupedStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.DestinationState;
        }

        public DestinationState from(NoPlatformStationState noPlatformStation, Duration cost, GraphNode node) {
            return new DestinationState(noPlatformStation, cost, node, this);
        }

        public DestinationState from(WalkingState walkingState, Duration cost, GraphNode node) {
            return new DestinationState(walkingState, cost, node, this);
        }

        public DestinationState from(PlatformStationState platformStationState, Duration cost, GraphNode node) {
            return new DestinationState(platformStationState, cost, node, this);
        }

        public DestinationState from(GroupedStationState groupedStationState, Duration cost, GraphNode node) {
            return new DestinationState(groupedStationState, cost, node, this);
        }

    }

    private DestinationState(TraversalState parent, Duration cost, GraphNode node, Towards<DestinationState> builder) {
        super(parent, Stream.empty(), cost, builder.getDestination(), node);
    }

    @Override
    public String toString() {
        return "DestinationState{} " + super.toString();
    }

}
