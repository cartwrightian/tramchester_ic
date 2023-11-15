package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.stateMachine.states.RouteStationState;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public abstract class TowardsRouteStation<T extends RouteStationState> implements Towards<T> {

    private final boolean interchangesOnly;

    public TowardsRouteStation(boolean interchangesOnly) {
        this.interchangesOnly = interchangesOnly;
    }

    protected <R extends ImmutableGraphRelationship> OptionalResourceIterator<R> getTowardsDestination(TraversalOps traversalOps,
                                                                                                       GraphNode node, TramDate date, GraphTransaction txn) {
        Stream<R> relationships = node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        return traversalOps.getTowardsDestination(Stream.concat(relationships, getActiveDiversions(node, date, txn)));
    }

    // TODO When to follow diversion departs? Should these be (also) INTERCHANGE_DEPART ?
    protected <R extends ImmutableGraphRelationship>  Stream<R> getOutboundsToFollow(GraphNode node, boolean isInterchange, TramDate date, GraphTransaction txn) {
        Stream<R> outboundsToFollow = Stream.empty();
        if (interchangesOnly) {
            if (isInterchange) {
                outboundsToFollow = node.getRelationships(txn, OUTGOING, INTERCHANGE_DEPART);
            }
        } else {
            outboundsToFollow = node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART);
        }

        Stream<R> diversions = getActiveDiversions(node, date, txn);
        return Stream.concat(outboundsToFollow, diversions);

//        if (diversions.isEmpty()) {
//            return outboundsToFollow;
//        } else {
//            return Stream.concat(outboundsToFollow, diversions.stream());
//        }
    }

    private <R extends GraphRelationship> Stream<R> getActiveDiversions(GraphNode node, TramDate date, GraphTransaction txn) {
        Stream<R> diversions = node.getRelationships(txn, OUTGOING, DIVERSION_DEPART);
        return diversions.filter(relationship -> relationship.validOn(date));
    }


}
