package com.tramchester.graph.search.stateMachine.states.station;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.search.stateMachine.journeyState.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsStation;
import com.tramchester.graph.search.stateMachine.journeyState.TraversalStateType;
import com.tramchester.graph.search.stateMachine.states.*;

import java.util.stream.Stream;

import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.TransportRelationshipTypes.*;

public class PlatformStationState extends StationState {

    public static class Builder extends StationBuilder<PlatformStationState>  {

        public Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
        }

        @Override
        public void register(RegistersFromState registers) {
            super.register(registers);
            registers.add(TraversalStateType.PlatformState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.PlatformStationState;
        }

        @Override
        public PlatformStationState fromWalking(final WalkingState walkingState, final GraphNode stationNode, final TramDuration cost,
                                                final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            final ImmutableEnumSet<TransportRelationshipTypes> fromWalking = ImmutableEnumSet.of(ENTER_PLATFORM, GROUPED_TO_PARENT, NEIGHBOUR);
            final Stream<GraphRelationship> initial = stationNode.getRelationships(txn, Outgoing, fromWalking);

            final Stream<GraphRelationship> relationships = addValidDiversions(initial, stationNode, false, txn);
            return new PlatformStationState(walkingState, relationships, cost, stationNode, journeyState, this);
        }

        public PlatformStationState fromPlatform(final PlatformState platformState, final GraphNode stationNode, final TramDuration cost,
                                                 final JourneyStateUpdate journeyState, final GraphTransaction txn) {
            final ImmutableEnumSet<TransportRelationshipTypes> fromPlatform = ImmutableEnumSet.of(WALKS_FROM_STATION, ENTER_PLATFORM,
                    NEIGHBOUR, GROUPED_TO_PARENT);
            final Stream<GraphRelationship> initial = stationNode.getRelationships(txn, Outgoing, fromPlatform);
            final Stream<GraphRelationship> relationships = addValidDiversions(initial, stationNode, false, txn);

            return new PlatformStationState(platformState, filterExcludingNode(txn, relationships, platformState), cost,
                    stationNode, journeyState, this);
        }

        @Override
        public PlatformStationState fromStart(final NotStartedState notStartedState, final GraphNode stationNode, final TramDuration cost,
                                              final JourneyStateUpdate journeyState,
                                              final boolean arrivedViaDiversion,
                                              final GraphTransaction txn) {
            final ImmutableEnumSet<TransportRelationshipTypes> fromStart = ImmutableEnumSet.of(WALKS_FROM_STATION, GROUPED_TO_PARENT, ENTER_PLATFORM, NEIGHBOUR);
            final Stream<GraphRelationship> initial = stationNode.getRelationships(txn, Outgoing, fromStart);
            final Stream<GraphRelationship> relationships = addValidDiversions(initial, stationNode, arrivedViaDiversion, txn);

            return new PlatformStationState(notStartedState, relationships, cost, stationNode, journeyState, this);
        }

        @Override
        public PlatformStationState fromNeighbour(final StationState stationState, final GraphNode stationNode, final TramDuration cost,
                                                  final JourneyStateUpdate journeyState, final GraphTransaction txn,
                                                  final boolean viaDivert) {
            final ImmutableEnumSet<TransportRelationshipTypes> fromNeighbour = ImmutableEnumSet.of(ENTER_PLATFORM, GROUPED_TO_PARENT);

            final Stream<GraphRelationship> initial = stationNode.getRelationships(txn, Outgoing, fromNeighbour);

            final Stream<GraphRelationship> relationships = addValidDiversions(initial, stationNode, viaDivert, txn);

            return new PlatformStationState(stationState, relationships, cost, stationNode, journeyState, this);
        }

        @Override
        public PlatformStationState fromGrouped(final GroupedStationState groupedStationState, final GraphNode stationNode,
                                                final TramDuration cost,
                                                final JourneyStateUpdate journeyState, final GraphTransaction txn,
                                                boolean viaDivert) {

            // TODO Deal with Diverts here?

            final ImmutableEnumSet<TransportRelationshipTypes> fromGrouped = ImmutableEnumSet.of(ENTER_PLATFORM, GROUPED_TO_PARENT);

            final Stream<GraphRelationship> relationships = stationNode.getRelationships(txn, Outgoing, fromGrouped);
            return new PlatformStationState(groupedStationState, relationships, cost, stationNode, journeyState, this);
        }

    }

    private PlatformStationState(final ImmutableTraversalState parent, final Stream<GraphRelationship> relationships,
                                 final TramDuration cost, final GraphNode stationNode,
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
    protected TraversalState toPlatform(final PlatformState.Builder towardsPlatform, final GraphNode node,
                                        final TramDuration cost, final JourneyStateUpdate journeyState) {
        return towardsPlatform.from(this, node, cost, txn);
    }

}
