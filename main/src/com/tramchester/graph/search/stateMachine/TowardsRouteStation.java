package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.search.stateMachine.states.RouteStationState;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public abstract class TowardsRouteStation<T extends RouteStationState> implements Towards<T> {

    private final boolean interchangesOnly;

    public TowardsRouteStation(boolean interchangesOnly) {
        this.interchangesOnly = interchangesOnly;
    }

    protected OptionalResourceIterator<ImmutableGraphRelationship> getTowardsDestination(
            final TraversalOps traversalOps, final GraphNode node, final TramDate date, final GraphTransaction txn) {
        final Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        return traversalOps.getTowardsDestination(Stream.concat(relationships, getActiveDiversions(node, date, txn)));
    }

    // TODO When to follow diversion departs? Should these be (also) INTERCHANGE_DEPART ?
    protected Stream<ImmutableGraphRelationship> getOutboundsToFollow(final GraphNode node, final boolean isInterchange,
                                                                      final TramDate date, final GraphTransaction txn) {
        final Stream<ImmutableGraphRelationship> outboundsToFollow;
        if (interchangesOnly) {
            if (isInterchange) {
                outboundsToFollow = node.getRelationships(txn, OUTGOING, INTERCHANGE_DEPART);
            } else {
                outboundsToFollow = Stream.empty();
            }
        } else {
            outboundsToFollow = node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART);
        }

        final Stream<ImmutableGraphRelationship> diversions = getActiveDiversions(node, date, txn);
        return Stream.concat(outboundsToFollow, diversions);

    }

    private Stream<ImmutableGraphRelationship> getActiveDiversions(final GraphNode node, final TramDate date, final GraphTransaction txn) {
        final Stream<ImmutableGraphRelationship> diversions = node.getRelationships(txn, OUTGOING, DIVERSION_DEPART);
        return diversions.filter(relationship -> relationship.validOn(date));
    }


}
