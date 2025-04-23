package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.FilterByDestinations;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public static class Builder extends StateBuilder<WalkingState> {

        protected Builder(StateBuilderParameters parameters) {
            super(parameters);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.NotStartedState, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.PlatformStationState, this);

            // todo needed?
            //registers.add(GroupedStationState.class, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.WalkingState;
        }

        public TraversalState fromStart(final NotStartedState notStartedState, final GraphNode firstNode, final Duration cost, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = firstNode.getRelationships(txn, OUTGOING, WALKS_TO_STATION);
            final FilterByDestinations<ImmutableGraphRelationship> towardsDest = super.getTowardsDestinationFromWalk(txn, firstNode);

            // prioritise a direct walk from start if one is available
            if (towardsDest.isEmpty()) {
                return new WalkingState(notStartedState, relationships, cost, this, firstNode);
            } else {
                // direct
                return new WalkingState(notStartedState, towardsDest.stream(), cost, this, firstNode);
            }
        }

        public TraversalState fromStation(final StationState station, final GraphNode node, final Duration cost, final GraphTransaction txn) {
            return new WalkingState(station,
                    filterExcludingNode(txn, node.getRelationships(txn, OUTGOING), station), cost, this, node);
        }

    }

    private WalkingState(final ImmutableTraversalState parent, final Stream<ImmutableGraphRelationship> relationships, final Duration cost,
                         final Towards<WalkingState> builder, GraphNode graphNode) {
        super(parent, relationships, cost, builder.getDestination(), graphNode.getId());
    }

    @Override
    public String toString() {
        return "WalkingState{} " + super.toString();
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                     final JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState, txn);
    }

    @Override
    protected void toDestination(final DestinationState.Builder towardsDestination, final GraphNode node, final Duration cost,
                                 final JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        towardsDestination.from(this, cost, node);
    }
}
