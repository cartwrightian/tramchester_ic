package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.RouteCalculatorSupport;
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

        public TraversalState fromRouteStation(final RouteStationStateOnTrip state, final IdFor<Trip> tripId, final GraphNode graphNode,
                                               final Duration cost, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> hourRelationships = getHourRelationships(graphNode, txn);
            return new ServiceState(state, hourRelationships, ExistingTrip.onTrip(tripId), cost, this, graphNode);
        }

        public TraversalState fromRouteStation(final RouteStationStateEndTrip endTrip, final GraphNode graphNode, final Duration cost, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> hourRelationships = getHourRelationships(graphNode, txn);
            return new ServiceState(endTrip, hourRelationships, cost, this, graphNode);
        }

        public TraversalState fromRouteStation(final JustBoardedState justBoarded, final GraphNode graphNode, final Duration cost, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> hourRelationships = getHourRelationships(graphNode, txn);
            return new ServiceState(justBoarded, hourRelationships, cost, this, graphNode);
        }

        private Stream<ImmutableGraphRelationship> getHourRelationships(final GraphNode node, final GraphTransaction txn) {
            return node.getRelationships(txn, OUTGOING, TO_HOUR);
        }

    }

    private final ExistingTrip maybeExistingTrip;

    private ServiceState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships, final ExistingTrip maybeExistingTrip,
                         final Duration cost, final Towards<ServiceState> builder, GraphNode graphNode) {
        super(parent, relationships, cost, builder.getDestination(), graphNode);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    private ServiceState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships,
                         final Duration cost, final Towards<ServiceState> builder, GraphNode graphNode) {
        super(parent, relationships, cost, builder.getDestination(), graphNode);
        this.maybeExistingTrip = ExistingTrip.none();
    }

    @Override
    protected HourState toHour(final HourState.Builder towardsHour, final GraphNode node, final Duration cost) {
        return towardsHour.fromService(this, node, cost, maybeExistingTrip, txn);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getOutbounds(GraphTransaction txn, final RouteCalculatorSupport.PathRequest pathRequest) {
        final TramTime queryTime = pathRequest.getActualQueryTime();
        final int queryHour = queryTime.getHourOfDay();

        return super.getOutbounds(txn, pathRequest).sorted(TramTime.RollingHourComparator(queryHour,
                relationship -> {
                    final GraphNode endNode = relationship.getEndNode(txn);
                    return hourFor(endNode);
                }));

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
