package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphDirection;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationshipNeo4J;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.FilterByDestinations;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;

public class FindStateAfterRouteStation  {

    public TraversalState endTripTowardsStation(final TraversalStateType destination, final RouteStationStateEndTrip routeStationState,
                                                final GraphNode node, final Duration cost, final JourneyStateUpdate journeyStateUpdate,
                                                final GraphTransactionNeo4J txn, StateBuilder<?> stateBuilder) {
        final FilterByDestinations<ImmutableGraphRelationshipNeo4J> towardsDest = getTowardsDestination(stateBuilder,
                node, txn, false);
        if (!towardsDest.isEmpty()) {
            return createNoPlatformStationState(routeStationState, node, cost, journeyStateUpdate, towardsDest.stream(), destination);
        }
        // end of a trip, may need to go back to this route station to catch new service

        final Stream<ImmutableGraphRelationshipNeo4J> boardsAndOthers = getBoardsAndOthers(node, txn, false);
        final Stream<ImmutableGraphRelationshipNeo4J> relationships = stateBuilder.addValidDiversions(boardsAndOthers, node, journeyStateUpdate, txn);

        return createNoPlatformStationState(routeStationState, node, cost, journeyStateUpdate, relationships, destination);
    }

    public TraversalState endTripTowardsPlatform(final TraversalStateType towardsState, final RouteStationStateEndTrip routeStationState,
                                                 final GraphNode node, final Duration cost, final GraphTransactionNeo4J txn, StateBuilder<?> stateBuilder) {
        final FilterByDestinations<ImmutableGraphRelationshipNeo4J> towardsDest = getTowardsDestination(stateBuilder,
                node, txn, true);
        if (!towardsDest.isEmpty()) {
            return createPlatformState(towardsState, routeStationState, node, cost, towardsDest.stream());
        }

        final Stream<ImmutableGraphRelationshipNeo4J> platformRelationships = getBoardsAndOthers(node, txn, true);

        return createPlatformState(towardsState, routeStationState, node, cost, platformRelationships);
    }

    public TraversalState onTripTowardsStation(final TraversalStateType destination, final RouteStationStateOnTrip onTrip, final GraphNode node,
                                               final Duration cost, final JourneyStateUpdate journeyState, final GraphTransactionNeo4J txn, StateBuilder<?> stateBuilder) {
        final FilterByDestinations<ImmutableGraphRelationshipNeo4J> towardsDest = getTowardsDestination(stateBuilder, node, txn, false);
        if (!towardsDest.isEmpty()) {
            return createNoPlatformStationState(onTrip, node, cost, journeyState, towardsDest.stream(), destination);
        }

        // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
        final Stream<ImmutableGraphRelationshipNeo4J> relationships = getBoardsAndOthers(node, txn, false);
        final Stream<ImmutableGraphRelationshipNeo4J> filteredRelationships = stateBuilder.filterExcludingNode(txn, relationships, onTrip);
        return createNoPlatformStationState(onTrip, node, cost, journeyState, filteredRelationships, destination);
    }

    public TraversalState onTripTowardsPlatform(final TraversalStateType towardsState, final RouteStationStateOnTrip routeStationStateOnTrip,
                                                final GraphNode node, final Duration cost, final GraphTransactionNeo4J txn, StateBuilder<?> stateBuilder) {
        final FilterByDestinations<ImmutableGraphRelationshipNeo4J> towardsDest = getTowardsDestination(stateBuilder, node, txn, true);
        if (!towardsDest.isEmpty()) {
            return new PlatformState(routeStationStateOnTrip, towardsDest.stream(), node, cost, towardsState);
        }

        final Stream<ImmutableGraphRelationshipNeo4J> platformRelationships = getBoardsAndOthers(node, txn, true);

        // Cannot filter here as might be starting a new trip from this point, so need to 'go back' to the route station
        //Stream<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationStateOnTrip);

        return new PlatformState(routeStationStateOnTrip, platformRelationships, node, cost, towardsState);
    }

    private Stream<ImmutableGraphRelationshipNeo4J> getBoardsAndOthers(final GraphNode node, final GraphTransactionNeo4J txn, final boolean isPlatform) {

        final Stream<ImmutableGraphRelationshipNeo4J> other;
        if (isPlatform) {
            other = node.getRelationships(txn, GraphDirection.Outgoing, LEAVE_PLATFORM);
        } else {
            other = node.getRelationships(txn, GraphDirection.Outgoing, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
        }

        final Stream<ImmutableGraphRelationshipNeo4J> boarding = getBoardingRelationships(txn, node);

        // Note: Sorting by route connections is slow and produces little gain
        // final Stream<ImmutableGraphRelationship> boarding = traversalOps.orderBoardingRelationsByRouteConnections(unsorted);

        // order matters here when using depth first, i.e. explore walks first other will not follow those linked when solution found
        return Stream.concat(other, boarding);
    }

    public Stream<ImmutableGraphRelationshipNeo4J> getBoardingRelationships(final GraphTransactionNeo4J txn, final GraphNode node) {
        // TODO Order here?
        // towards route stations
        return node.getRelationships(txn, GraphDirection.Outgoing, BOARD, INTERCHANGE_BOARD);
    }

    private FilterByDestinations<ImmutableGraphRelationshipNeo4J> getTowardsDestination(final StateBuilder<?> stateBuilder,
                                                                                        final GraphNode node, final GraphTransactionNeo4J txn,
                                                                                        boolean isPlatform) {
        if (isPlatform) {
            return stateBuilder.getTowardsDestinationFromPlatform(txn, node);
        } else {
            return stateBuilder.getTowardsDestinationFromNonPlatformStation(txn, node);
        }
    }

    private NoPlatformStationState createNoPlatformStationState(ImmutableTraversalState parentState, GraphNode node,
                                                                Duration cost, JourneyStateUpdate journeyStateUpdate,
                                                                Stream<ImmutableGraphRelationshipNeo4J> relationships,
                                                                TraversalStateType towardsState) {
        return new NoPlatformStationState(parentState, relationships, cost, node, journeyStateUpdate, towardsState);
    }

    private static PlatformState createPlatformState(TraversalStateType towardsState, RouteStationStateEndTrip routeStationState,
                                                     GraphNode node, Duration cost, Stream<ImmutableGraphRelationshipNeo4J> platformRelationships) {
        return new PlatformState(routeStationState, platformRelationships, node, cost, towardsState);
    }
}
