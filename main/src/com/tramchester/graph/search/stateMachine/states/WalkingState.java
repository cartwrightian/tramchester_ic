package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
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

        public TraversalState fromStart(NotStartedState notStartedState, GraphNode firstNode, Duration cost, MutableGraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = firstNode.getRelationships(txn, OUTGOING, WALKS_TO_STATION);
            final List<ImmutableGraphRelationship> needTwice = relationships.toList();
            OptionalResourceIterator<ImmutableGraphRelationship> towardsDest = notStartedState.traversalOps.getTowardsDestination(needTwice.stream());

            // prioritise a direct walk from start if one is available
            if (towardsDest.isEmpty()) {
                return new WalkingState(notStartedState, needTwice.stream(), cost, this);
            } else {
                // direct
                return new WalkingState(notStartedState, towardsDest.stream(), cost, this);
            }
        }

        public TraversalState fromStation(StationState station, GraphNode node, Duration cost, MutableGraphTransaction txn) {
            return new WalkingState(station,
                    filterExcludingEndNode(txn, node.getRelationships(txn, OUTGOING), station), cost, this);
        }

    }

    private WalkingState(TraversalState parent, Stream<ImmutableGraphRelationship> relationships, Duration cost, Towards<WalkingState> builder) {
        super(parent, relationships, cost, builder.getDestination());
    }

//    private WalkingState(TraversalState parent, ResourceIterable<Relationship> relationships, Duration cost, Towards<WalkingState> builder) {
//        super(parent, relationships, cost, builder.getDestination());
//    }

    @Override
    public String toString() {
        return "WalkingState{} " + super.toString();
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState, txn);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState, txn);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, GraphNode node, Duration cost,
                                 JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        towardsDestination.from(this, cost);
    }
}
