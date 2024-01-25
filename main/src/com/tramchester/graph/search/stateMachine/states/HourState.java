package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_MINUTE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class HourState extends TraversalState {

    public static class Builder implements Towards<HourState> {

        public HourState fromService(final ServiceState serviceState, final GraphNode node, final Duration cost,
                                     final ExistingTrip maybeExistingTrip, final GraphTransaction txn) {
            final Stream<ImmutableGraphRelationship> relationships = getMinuteRelationships(node, txn);
            return new HourState(serviceState, relationships, node, maybeExistingTrip, cost, this);
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
            Stream<ImmutableGraphRelationship> unsorted = getRelationships(txn, node, OUTGOING, TO_MINUTE);
            // NOTE: need an ordering here to produce consistent results, time is as good as any and no obvious way to optimise
            // the order here, unlike for HOURS
            return unsorted.sorted(Comparator.comparing(relationship -> relationship.getEndNode(txn).getTime()));
        }
    }

    private final ExistingTrip maybeExistingTrip;

    private HourState(final TraversalState parent, final Stream<ImmutableGraphRelationship> relationships,
                      final GraphNode node,
                      final ExistingTrip maybeExistingTrip, final Duration cost, final Towards<HourState> builder) {
        super(parent, relationships, cost, builder.getDestination(), node);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    @Override
    protected TraversalState toMinute(final MinuteState.Builder towardsMinute, final GraphNode minuteNode, final Duration cost,
                                      final JourneyStateUpdate journeyState, final TransportRelationshipTypes[] currentModes) {
        try {
            final TramTime time = traversalOps.getTimeFrom(minuteNode);
            journeyState.recordTime(time, getTotalDuration());
        } catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process time ordering", exception);
        }

        return towardsMinute.fromHour(this, minuteNode, cost, maybeExistingTrip, journeyState, currentModes, txn);
    }

    @Override
    public String toString() {
        return "HourState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }
}
