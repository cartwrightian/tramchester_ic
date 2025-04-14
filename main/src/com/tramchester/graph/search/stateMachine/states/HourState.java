package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_MINUTE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class HourState extends TraversalState implements HasTowardsStationId {

    public static class Builder extends StateBuilder<HourState> {

        private final boolean depthFirst;

        public Builder(StateBuilderParameters builderParameters) {
            super(builderParameters);
            this.depthFirst = builderParameters.depthFirst();
        }

        public HourState fromService(final ServiceState serviceState, final GraphNode node, final Duration cost,
                                     final IdFor<Station> towardsStationId, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = getMinuteRelationships(node, txn);
            return new HourState(serviceState, relationships, node, towardsStationId, cost, this);
        }

        @Override
        public void register(final RegistersFromState registers) {
            registers.add(TraversalStateType.ServiceState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.HourState;
        }

        private Stream<ImmutableGraphRelationship> getMinuteRelationships(final GraphNode node, final GraphTransaction txn) {
            Stream<ImmutableGraphRelationship> unsorted = node.getRelationships(txn, OUTGOING, TO_MINUTE);
            if (depthFirst) {
                // NOTE: need an ordering here to produce consistent results, time is as good as any and no obvious way to optimise
                // the order here, unlike for HOURS
                return unsorted.sorted(Comparator.comparing(relationship -> relationship.getEndNode(txn).getTime()));
            } else {
                return unsorted;
            }

        }
    }

    private final IdFor<Station> towardsStationId;

    private HourState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships,
                      final GraphNode node, IdFor<Station> towardsStationId, final Duration cost, final Towards<HourState> builder) {
        super(parent, relationships, cost, builder.getDestination(), node.getId());
        this.towardsStationId = towardsStationId;
    }

    @Override
    protected TraversalState toMinute(final MinuteState.Builder towardsMinute, final GraphNode minuteNode, final Duration cost,
                                      final JourneyStateUpdate journeyState) {
        try {
            final TramTime time = minuteNode.getTime(); // super.getTimeFrom(minuteNode);
            journeyState.recordTime(time, getTotalDuration());
        } catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process time ordering", exception);
        }

        return towardsMinute.fromHour(this, minuteNode, cost, towardsStationId, journeyState, txn);
    }


    @Override
    public IdFor<Station> getTowards() {
        return towardsStationId;
    }

    @Override
    public String toString() {
        return "HourState{" +
                "} " + super.toString();
    }
}
