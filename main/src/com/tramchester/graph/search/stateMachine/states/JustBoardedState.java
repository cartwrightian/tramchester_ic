package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class JustBoardedState extends RouteStationState {

    public static class Builder extends TowardsRouteStation<JustBoardedState> {

        private final boolean depthFirst;

        public Builder(boolean depthFirst, boolean interchangesOnly) {
            super(interchangesOnly);
            this.depthFirst = depthFirst;
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

        public JustBoardedState fromPlatformState(final PlatformState platformState, final GraphNode node, final Duration cost, final GraphTransaction txn) {

            final Stream<ImmutableGraphRelationship> otherPlatforms = filterExcludingEndNode(txn, node.getRelationships(txn, OUTGOING, ENTER_PLATFORM),
                    platformState);

            final Stream<ImmutableGraphRelationship> services = getServices(node, txn);

            return new JustBoardedState(platformState, Stream.concat(services, otherPlatforms), cost, this);
        }

        public JustBoardedState fromNoPlatformStation(final NoPlatformStationState noPlatformStation, final GraphNode node,
                                                      final Duration cost, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> filteredDeparts = filterExcludingEndNode(txn,
                    node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART),
                    noPlatformStation);
            final Stream<ImmutableGraphRelationship> services = getServices(node, txn);
            return new JustBoardedState(noPlatformStation, Stream.concat(filteredDeparts, services), cost, this);
        }

        private static Stream<ImmutableGraphRelationship> getServices(final GraphNode node, final GraphTransaction txn) {
            // makes little difference?
//            if (depthFirst) {
//                services = orderServicesByRouteMetric(node, platformState.traversalOps, txn);
//            } else {
//                services = node.getRelationships(txn, OUTGOING, TO_SERVICE);
//            }
            return node.getRelationships(txn, OUTGOING, TO_SERVICE);
        }

        // TODO Service is likely the wrong level of prioritisation for changed structure of source data
        // really route is the right place to prioritize?
        // group many

//        /**
//         * order by least number connections required to destination routes
//         * @param node start node
//         * @param traversalOps supporting ops
//         * @param txn current transaction
//         * @return ordered by least number routes interconnects first
//         */
//        private Stream<ImmutableGraphRelationship> orderServicesByRouteMetric(final GraphNode node, final TraversalOps traversalOps, final GraphTransaction txn) {
//            final Stream<ImmutableGraphRelationship> toServices = getServices(node, txn);
//            return traversalOps.orderBoardingRelationsByRouteConnections(toServices);
//        }
//
//        /***
//         * Order outbound relationships by end node distance to destination
//         * significant overall performance increase for non-trivial geographically diverse networks
//         */
//        private Stream<GraphRelationship> orderServicesByDistance(final GraphNode node, final TraversalOps traversalOps, final GraphTransaction txn) {
//            final Stream<ImmutableGraphRelationship> toServices = getServices(node, txn);
//            return traversalOps.orderRelationshipsByDistance(toServices);
//        }

    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private JustBoardedState(final TraversalState traversalState, final Stream<ImmutableGraphRelationship> outbounds,
                             final Duration cost, final TowardsRouteStation<?> builder) {
        super(traversalState, outbounds, cost, builder);
    }

    @Override
    protected TraversalState toService(final ServiceState.Builder towardsService, final GraphNode node, final Duration cost) {
        return towardsService.fromRouteStation(this, node, cost, txn);
    }
}
