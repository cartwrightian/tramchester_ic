package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.OptionalResourceIterator;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public static class Builder implements Towards<WalkingState> {

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
            final List<ImmutableGraphRelationship> needTwice = relationships.toList();
            final OptionalResourceIterator<ImmutableGraphRelationship> towardsDest = notStartedState.traversalOps.getTowardsDestination(needTwice.stream());

            // prioritise a direct walk from start if one is available
            if (towardsDest.isEmpty()) {
                return new WalkingState(notStartedState, needTwice.stream(), cost, this, firstNode);
            } else {
                // direct
                return new WalkingState(notStartedState, towardsDest.stream(), cost, this, firstNode);
            }
        }

        public TraversalState fromStation(final StationState station, final GraphNode node, final Duration cost, final GraphTransaction txn) {
            return new WalkingState(station,
                    filterExcludingEndNode(txn, node.getRelationships(txn, OUTGOING), station), cost, this, node);
        }

    }

    private WalkingState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships, final Duration cost,
                         final Towards<WalkingState> builder, GraphNode graphNode) {
        super(parent, relationships, cost, builder.getDestination(), graphNode);
    }

//    private WalkingState(TraversalState parent, ResourceIterable<Relationship> relationships, Duration cost, Towards<WalkingState> builder) {
//        super(parent, relationships, cost, builder.getDestination());
//    }

    @Override
    public String toString() {
        return "WalkingState{} " + super.toString();
    }

    @Override
    protected PlatformStationState toPlatformStation(final PlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                     final JourneyStateUpdate journeyState, final boolean onDiversion) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(final NoPlatformStationState.Builder towardsStation, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyState, final boolean onDiversion) {
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
