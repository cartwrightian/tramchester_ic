package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.search.stateMachine.states.RouteStationState;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public abstract class TowardsRouteStation<T extends RouteStationState> implements Towards<T> {

    private final boolean interchangesOnly;

    public TowardsRouteStation(boolean interchangesOnly) {
        this.interchangesOnly = interchangesOnly;
    }

    protected OptionalResourceIterator<GraphRelationship> getTowardsDestination(TraversalOps traversalOps, GraphNode node, TramDate date) {
        Stream<GraphRelationship> relationships = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        return traversalOps.getTowardsDestination(Stream.concat(relationships, getActiveDiversions(node, date)));
    }

    // TODO When to follow diversion departs? Should these be (also) INTERCHANGE_DEPART ?
    protected Stream<GraphRelationship> getOutboundsToFollow(GraphNode node, boolean isInterchange, TramDate date) {
        Stream<GraphRelationship> outboundsToFollow = Stream.empty();
        if (interchangesOnly) {
            if (isInterchange) {
                outboundsToFollow = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
            }
        } else {
            outboundsToFollow = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);
        }

        List<GraphRelationship> diversions = getActiveDiversions(node, date).toList();
        if (diversions.isEmpty()) {
            return outboundsToFollow;
        } else {
            return Stream.concat(outboundsToFollow, diversions.stream());
        }
    }

    private Stream<GraphRelationship> getActiveDiversions(GraphNode node, TramDate date) {
        Stream<GraphRelationship> diversions = node.getRelationships(OUTGOING, DIVERSION_DEPART);
        return diversions.filter(relationship -> relationship.validOn(date));
    }


}
