package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphDirection;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;

public class ServiceState extends TraversalState implements HasTowardsStationId {

    public static class Builder extends StateBuilder<ServiceState> {

        private final boolean depthFirst;

        public Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
            this.depthFirst = builderParameters.depthFirst();
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

        public TraversalState fromRouteStation(final RouteStationStateOnTrip state, final GraphNode serviceNode,
                                               final Duration cost, final GraphTransactionNeo4J txn) {
            final Stream<ImmutableGraphRelationship> hourRelationships = getHourRelationships(serviceNode, txn);
            return new ServiceState(state, hourRelationships, cost, this, serviceNode, depthFirst,
                    super.getQueryHour());
        }

        public TraversalState fromRouteStation(final JustBoardedState justBoarded, final GraphNode serviceNode,
                                               final Duration cost, final GraphTransactionNeo4J txn) {
            final Stream<ImmutableGraphRelationship> hourRelationships = getHourRelationships(serviceNode, txn);
            return new ServiceState(justBoarded, hourRelationships, cost, this, serviceNode, depthFirst,
                    super.getQueryHour());
        }

        private Stream<ImmutableGraphRelationship> getHourRelationships(final GraphNode serviceNode, final GraphTransactionNeo4J txn) {
            return serviceNode.getRelationships(txn, GraphDirection.Outgoing, TO_HOUR);
        }

    }

    private final boolean depthFirst;
    private final int queryHour;
    private final IdFor<Station> towardsStationId;

    private ServiceState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships,
                         final Duration cost, final Towards<ServiceState> builder, GraphNode serviceNode, boolean depthFirst, int queryHour) {
        super(parent, relationships, cost, builder.getDestination(), serviceNode.getId());
        this.queryHour = queryHour;
        this.depthFirst = depthFirst;
        this.towardsStationId = serviceNode.getTowardsStationId();
    }

    @Override
    protected HourState toHour(final HourState.Builder towardsHour, final GraphNode node, final Duration cost) {
        return towardsHour.fromService(this, node, cost, towardsStationId, txn);
    }

    @Override
    public Stream<ImmutableGraphRelationship> getOutbounds() {
        if (depthFirst) {

            return super.getOutbounds().sorted(TramTime.RollingHourComparator(queryHour,
                    relationship -> {
                        final GraphNode endNode = relationship.getEndNode(txn);
                        return hourFor(endNode);
                    }));
        } else {
            return super.getOutbounds();
        }
    }

    private int hourFor(final GraphNode endNode) {
        return endNode.getHour();
        //return GraphLabel.getHourFrom(endNode.getLabels());
    }

    @Override
    public IdFor<Station> getTowards() {
        return towardsStationId;
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "} " + super.toString();
    }

}
