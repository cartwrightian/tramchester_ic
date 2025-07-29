package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.facade.GraphDirection;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationshipNeo4J;
import com.tramchester.graph.search.stateMachine.states.RouteStationState;
import com.tramchester.graph.search.stateMachine.states.StateBuilder;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;

public abstract class TowardsRouteStation<T extends RouteStationState> extends StateBuilder<T> {

    private final boolean interchangesOnly;

    public TowardsRouteStation(StateBuilderParameters builderParameters) {
        super(builderParameters);
        this.interchangesOnly = builderParameters.interchangesOnly();
    }

    protected FilterByDestinations<ImmutableGraphRelationshipNeo4J> getTowardsDestination(final GraphNode node, final GraphTransactionNeo4J txn) {
        return super.getTowardsDestinationFromRouteStation(node, txn);
    }

    // TODO When to follow diversion departs? Should these be (also) INTERCHANGE_DEPART ?
    protected Stream<ImmutableGraphRelationshipNeo4J> getOutboundsToFollow(final GraphNode node, final boolean isInterchange,
                                                                           final GraphTransactionNeo4J txn) {
        final Stream<ImmutableGraphRelationshipNeo4J> outboundsToFollow;
        if (interchangesOnly) {
            if (isInterchange) {
                outboundsToFollow = node.getRelationships(txn, GraphDirection.Outgoing, INTERCHANGE_DEPART);
            } else {
                outboundsToFollow = Stream.empty();
            }
        } else {
            // not only interchanges
            outboundsToFollow = node.getRelationships(txn, GraphDirection.Outgoing, DEPART, INTERCHANGE_DEPART);
        }

        // also follow any active diversions
        final Stream<ImmutableGraphRelationshipNeo4J> diversions = getActiveDiversions(node, txn);
        return Stream.concat(outboundsToFollow, diversions);

    }

    private Stream<ImmutableGraphRelationshipNeo4J> getActiveDiversions(final GraphNode node, final GraphTransactionNeo4J txn) {
        final TramDate queryDate = super.getQueryDate();

        final Stream<ImmutableGraphRelationshipNeo4J> diversions = node.getRelationships(txn, GraphDirection.Outgoing, DIVERSION_DEPART);
        return diversions.filter(relationship -> relationship.validOn(queryDate));
    }


}
