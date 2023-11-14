package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    public static class Builder implements Towards<ServiceState> {

        private final boolean depthFirst;
        private final NodeContentsRepository nodeContents;

        public Builder(boolean depthFirst, NodeContentsRepository nodeContents) {
            this.depthFirst = depthFirst;
            this.nodeContents = nodeContents;
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

        public TraversalState fromRouteStation(RouteStationStateOnTrip state, IdFor<Trip> tripId, GraphNode node, Duration cost, GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> serviceRelationships = getHourRelationships(node, txn);
            return new ServiceState(state, serviceRelationships, ExistingTrip.onTrip(tripId), cost, this);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip endTrip, GraphNode node, Duration cost, GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> serviceRelationships = getHourRelationships(node, txn);
            return new ServiceState(endTrip, serviceRelationships, cost, this);
        }

        public TraversalState fromRouteStation(JustBoardedState justBoarded, GraphNode node, Duration cost, GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> serviceRelationships = getHourRelationships(node, txn);
            return new ServiceState(justBoarded, serviceRelationships, cost, this);
        }

        private Stream<ImmutableGraphRelationship> getHourRelationships(GraphNode node, GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, OUTGOING, TO_HOUR);
            if (depthFirst) {
                // todo is the gain here worth the overhead of computing the hour for the end node?
                return relationships.sorted(Comparator.comparingInt(
                        relationship -> {
                            final GraphNode endNode = relationship.getEndNode(txn);
                            return hourLabelFor(endNode);
                        }));
            }
            return relationships;
        }

        private int hourLabelFor(GraphNode endNode) {
            return nodeContents.getHour(endNode);
        }

    }

    private final ExistingTrip maybeExistingTrip;

    private ServiceState(TraversalState parent, Stream<ImmutableGraphRelationship> relationships, ExistingTrip maybeExistingTrip,
                         Duration cost, Towards<ServiceState> builder) {
        super(parent, relationships, cost, builder.getDestination());
        this.maybeExistingTrip = maybeExistingTrip;
    }

    private ServiceState(TraversalState parent, Stream<ImmutableGraphRelationship> relationships, Duration cost, Towards<ServiceState> builder) {
        super(parent, relationships, cost, builder.getDestination());
        this.maybeExistingTrip = ExistingTrip.none();
    }

    @Override
    protected HourState toHour(HourState.Builder towardsHour, GraphNode node, Duration cost) {
        return towardsHour.fromService(this, node, cost, maybeExistingTrip, txn);
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }

}
