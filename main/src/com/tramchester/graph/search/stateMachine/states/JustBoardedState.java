package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.*;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import com.tramchester.graph.search.stateMachine.TraversalOps;

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

        public JustBoardedState fromPlatformState(PlatformState platformState, GraphNode node, Duration cost, GraphTransaction txn) {
            // does this ever happen? Get on one route station only to go back to a whole different
            // platform?
            Stream<ImmutableGraphRelationship> otherPlatforms = filterExcludingEndNode(txn, node.getRelationships(txn, OUTGOING, ENTER_PLATFORM),
                    platformState);

            Stream<ImmutableGraphRelationship> services;
            if (depthFirst) {
                services = orderServicesByRouteMetric(node, platformState.traversalOps, txn);
            } else {
                services = node.getRelationships(txn, OUTGOING, TO_SERVICE);
            }

            return new JustBoardedState(platformState, Stream.concat(services, otherPlatforms), cost, this);
        }

        public JustBoardedState fromNoPlatformStation(NoPlatformStationState noPlatformStation, GraphNode node, Duration cost, GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> filteredDeparts = filterExcludingEndNode(txn,
                    node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART),
                    noPlatformStation);
            Stream<ImmutableGraphRelationship> services = orderServicesByRouteMetric(node, noPlatformStation.traversalOps, txn);
            return new JustBoardedState(noPlatformStation, Stream.concat(filteredDeparts, services), cost, this);
        }

        /**
         * order by least number connections required to destination routes
         * @param node start node
         * @param traversalOps supporting ops
         * @param txn currnet transaction
         * @return ordered by least number routes interconnects first
         */
        private Stream<ImmutableGraphRelationship> orderServicesByRouteMetric(GraphNode node, TraversalOps traversalOps, GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> toServices = node.getRelationships(txn, OUTGOING, TO_SERVICE);
            return traversalOps.orderBoardingRelationsByRouteConnections(toServices);
        }

        /***
         * Order outbound relationships by end node distance to destination
         * significant overall performance increase for non-trivial geographically diverse networks
         */
        private Stream<GraphRelationship> orderServicesByDistance(GraphNode node, TraversalOps traversalOps, GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> toServices = node.getRelationships(txn, OUTGOING, TO_SERVICE);
            return traversalOps.orderRelationshipsByDistance(toServices);
        }

    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private JustBoardedState(TraversalState traversalState, Stream<ImmutableGraphRelationship> outbounds, Duration cost, TowardsRouteStation<?> builder) {
        super(traversalState, outbounds, cost, builder);
    }

    @Override
    protected TraversalState toService(ServiceState.Builder towardsService, GraphNode node, Duration cost) {
        return towardsService.fromRouteStation(this, node, cost, txn);
    }
}
