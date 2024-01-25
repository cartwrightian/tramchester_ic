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

        public Builder(boolean interchangesOnly) {
            super(interchangesOnly);
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

        public JustBoardedState fromPlatformState(final PlatformState platformState, final GraphNode routeStationNode, final Duration cost, final GraphTransaction txn) {

            final Stream<ImmutableGraphRelationship> services = getServices(routeStationNode, txn);

            // todo these never get used, otherwise would see a call to the base toPlatformStation and hence a runtime exception
//            final Stream<ImmutableGraphRelationship> otherPlatforms = filterExcludingEndNode(txn, routeStationNode.getRelationships(txn, OUTGOING, ENTER_PLATFORM),
//                    platformState);
//            Stream<ImmutableGraphRelationship> both = Stream.concat(services, otherPlatforms);
            return new JustBoardedState(platformState, services, cost, this, routeStationNode);
        }

        public JustBoardedState fromNoPlatformStation(final NoPlatformStationState noPlatformStation, final GraphNode routeStationNode,
                                                      final Duration cost, final GraphTransaction txn) {

            final Stream<ImmutableGraphRelationship> services = getServices(routeStationNode, txn);

            // todo these never get used, otherwise would see a call to the base toNoPlatformStation and hence a runtime exception
//            final Stream<ImmutableGraphRelationship> filteredDeparts = filterExcludingEndNode(txn,
//                    routeStationNode.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART),
//                    noPlatformStation);
//            Stream<ImmutableGraphRelationship> both = Stream.concat(services, filteredDeparts);
            return new JustBoardedState(noPlatformStation, services, cost, this, routeStationNode);
        }

        private static Stream<ImmutableGraphRelationship> getServices(final GraphNode routeStationNode, final GraphTransaction txn) {
            // not sorted, only see one svc outbound from a route station node
            return routeStationNode.getRelationships(txn, OUTGOING, TO_SERVICE);
        }
    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private JustBoardedState(final TraversalState traversalState, final Stream<ImmutableGraphRelationship> outbounds,
                             final Duration cost, final TowardsRouteStation<?> builder, GraphNode graphNode) {
        super(traversalState, outbounds, cost, builder, graphNode);
    }

    @Override
    protected TraversalState toService(final ServiceState.Builder towardsService, final GraphNode node, final Duration cost) {
        return towardsService.fromRouteStation(this, node, cost, txn);
    }
}
