package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;

import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.TO_SERVICE;

public class JustBoardedState extends RouteStationState {

    public static class Builder extends TowardsRouteStation<JustBoardedState> {

        public Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.PlatformState, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.JustBoardedState;
        }

        public JustBoardedState fromPlatformState(JourneyStateUpdate journeyState, final PlatformState platformState, final GraphNode routeStationNode,
                                                  final TramDuration cost, final GraphTransaction txn) {

            final Stream<GraphRelationship> services = getServices(routeStationNode, txn);

            return new JustBoardedState(platformState, services, journeyState, cost, this, routeStationNode);
        }

        public JustBoardedState fromNoPlatformStation(JourneyStateUpdate journeyState, final NoPlatformStationState noPlatformStation, final GraphNode routeStationNode,
                                                      final TramDuration cost, final GraphTransaction txn) {

            final Stream<GraphRelationship> services = getServices(routeStationNode, txn);

            return new JustBoardedState(noPlatformStation, services, journeyState, cost, this, routeStationNode);
        }

        private static Stream<GraphRelationship> getServices(final GraphNode routeStationNode, final GraphTransaction txn) {
            // not sorted, only see one svc outbound from a route station node
            return routeStationNode.getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE);
        }
    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private JustBoardedState(final ImmutableTraversalState traversalState, final Stream<GraphRelationship> outbounds,
                             JourneyStateUpdate journeyState, final TramDuration cost, final TowardsRouteStation<?> builder, GraphNode graphNode) {
        super(traversalState, outbounds, journeyState, cost, builder, graphNode);
    }

    @Override
    protected TraversalState toService(final ServiceState.Builder towardsService, final GraphNode serviceNode, final TramDuration cost) {
        return towardsService.fromRouteStation(this, serviceNode, cost, txn);
    }
}
