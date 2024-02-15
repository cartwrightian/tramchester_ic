package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    public static class Builder implements Towards<ServiceState> {

        private final boolean depthFirst;

        public Builder(boolean depthFirst) {
            this.depthFirst = depthFirst;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.RouteStationStateOnTrip, this);
            registers.add(TraversalStateType.RouteStationStateEndTrip, this);
            registers.add(TraversalStateType.JustBoardedState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.ServiceState;
        }

        public TraversalState fromRouteStation(final RouteStationStateOnTrip state, final IdFor<Trip> tripId, final GraphNode graphNode,
                                               final Duration cost, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> hourRelationships = getHourRelationships(graphNode, txn);
            return new ServiceState(state, hourRelationships, ExistingTrip.onTrip(tripId), cost, this, graphNode, depthFirst,
                    state.traversalOps.getQueryHour());
        }

        public TraversalState fromRouteStation(final JustBoardedState justBoarded, final GraphNode graphNode, final Duration cost, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> hourRelationships = getHourRelationships(graphNode, txn);
            return new ServiceState(justBoarded, hourRelationships, cost, this, graphNode, depthFirst, justBoarded.traversalOps.getQueryHour());
        }

        private Stream<ImmutableGraphRelationship> getHourRelationships(final GraphNode node, final GraphTransaction txn) {
            return node.getRelationships(txn, OUTGOING, TO_HOUR);
        }

    }

    private final ExistingTrip maybeExistingTrip;
    private final boolean depthFirst;
    private final int queryHour;

    private ServiceState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships, final ExistingTrip maybeExistingTrip,
                         final Duration cost, final Towards<ServiceState> builder, GraphNode graphNode, boolean depthFirst, int queryHour) {
        super(parent, relationships, cost, builder.getDestination(), graphNode);
        this.maybeExistingTrip = maybeExistingTrip;
        this.depthFirst = depthFirst;
        this.queryHour = queryHour;
    }

    private ServiceState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships,
                         final Duration cost, final Towards<ServiceState> builder, GraphNode graphNode, boolean depthFirst, int queryHour) {
        super(parent, relationships, cost, builder.getDestination(), graphNode);
        this.queryHour = queryHour;
        this.maybeExistingTrip = ExistingTrip.none();
        this.depthFirst = depthFirst;
    }

    @Override
    protected HourState toHour(final HourState.Builder towardsHour, final GraphNode node, final Duration cost) {
        return towardsHour.fromService(this, node, cost, maybeExistingTrip, txn);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getOutbounds(final GraphTransaction txn) {
        if (depthFirst) {

            return super.getOutbounds(txn).sorted(TramTime.RollingHourComparator(queryHour,
                    relationship -> {
                        final GraphNode endNode = relationship.getEndNode(txn);
                        return hourFor(endNode);
                    }));
        } else {
            return super.getOutbounds(txn);
        }
    }

    private int hourFor(GraphNode endNode) {
        return GraphLabel.getHourFrom(endNode.getLabels());
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }

}
