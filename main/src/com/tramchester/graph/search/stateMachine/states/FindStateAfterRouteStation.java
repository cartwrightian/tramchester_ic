package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_PARENT;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class FindStateAfterRouteStation extends StationStateBuilder {

    public TraversalState fromRouteStationEndTrip(TraversalStateType destination, final RouteStationStateEndTrip routeStationState, final GraphNode node, final Duration cost,
                                                  final JourneyStateUpdate journeyState, final boolean alreadyOnDiversion, final GraphTransaction txn) {
        // end of a trip, may need to go back to this route station to catch new service
        final Stream<ImmutableGraphRelationship> initial = getBoardsAndOthers(node, txn);
        final Stream<ImmutableGraphRelationship> relationships = addValidDiversions(node, initial, routeStationState, alreadyOnDiversion, txn);
        return createNotPlatformStationState(routeStationState, node, cost, journeyState, relationships, destination);
    }


    public TraversalState fromRouteStationOnTrip(TraversalStateType destination, final RouteStationStateOnTrip onTrip, final GraphNode node, final Duration cost,
                                                 final JourneyStateUpdate journeyState, final GraphTransaction txn) {
        // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
        final Stream<ImmutableGraphRelationship> relationships = getBoardsAndOthers(node, txn);
        final Stream<ImmutableGraphRelationship> filteredRelationships = TraversalState.filterExcludingEndNode(txn, relationships, onTrip);
        return createNotPlatformStationState(onTrip, node, cost, journeyState, filteredRelationships, destination);
    }

    Stream<ImmutableGraphRelationship> getBoardsAndOthers(final GraphNode node, final GraphTransaction txn) {
        final Stream<ImmutableGraphRelationship> other = node.getRelationships(txn, OUTGOING, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);

        // todo need to order boarding relationships
        final Stream<ImmutableGraphRelationship> boarding = node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD);
        // order matters here, i.e. explore walks first
        return Stream.concat(other, boarding);
    }

    @NotNull
    private NoPlatformStationState createNotPlatformStationState(TraversalState parentState, GraphNode node,
                                                                 Duration cost, JourneyStateUpdate journeyState,
                                                                 Stream<ImmutableGraphRelationship> relationships, TraversalStateType destination) {
        return new NoPlatformStationState(parentState, relationships, cost, node, journeyState, destination);
    }
}
