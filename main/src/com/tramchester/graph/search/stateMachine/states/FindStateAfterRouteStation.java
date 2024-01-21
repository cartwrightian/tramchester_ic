package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.OptionalResourceIterator;
import com.tramchester.graph.search.stateMachine.TraversalOps;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class FindStateAfterRouteStation extends StationStateBuilder {

    public TraversalState endTripTowardsStation(TraversalStateType destination, final RouteStationStateEndTrip routeStationState,
                                                final GraphNode node, final Duration cost, final JourneyStateUpdate journeyState,
                                                final boolean alreadyOnDiversion, final GraphTransaction txn) {
        // end of a trip, may need to go back to this route station to catch new service

        final Stream<ImmutableGraphRelationship> boardsAndOthers = getBoardsAndOthers(node, txn, false, routeStationState.traversalOps);

        final Stream<ImmutableGraphRelationship> diversions = addValidDiversions(node, routeStationState, alreadyOnDiversion, txn);

        Stream<ImmutableGraphRelationship> relationships = Stream.concat(boardsAndOthers, diversions);
        return createNoPlatformStationState(routeStationState, node, cost, journeyState, relationships, destination);
    }

    public TraversalState endTripTowardsPlatform(TraversalStateType towardsState, RouteStationStateEndTrip routeStationState,
                                                 GraphNode node, Duration cost, GraphTransaction txn) {
        final OptionalResourceIterator<ImmutableGraphRelationship> towardsDest = getTowardsDestination(routeStationState.traversalOps, node, txn);
        if (!towardsDest.isEmpty()) {
            return createPlatformState(towardsState, routeStationState, node, cost, towardsDest.stream());
        }

        final Stream<ImmutableGraphRelationship> platformRelationships = getBoardsAndOthers(node, txn, true, routeStationState.traversalOps);

        return createPlatformState(towardsState, routeStationState, node, cost, platformRelationships);
    }

    public TraversalState onTripTowardsStation(TraversalStateType destination, final RouteStationStateOnTrip onTrip, final GraphNode node,
                                               final Duration cost, final JourneyStateUpdate journeyState, final GraphTransaction txn) {
        // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
        final Stream<ImmutableGraphRelationship> relationships = getBoardsAndOthers(node, txn, false, onTrip.traversalOps);
        final Stream<ImmutableGraphRelationship> filteredRelationships = TraversalState.filterExcludingEndNode(txn, relationships, onTrip);
        return createNoPlatformStationState(onTrip, node, cost, journeyState, filteredRelationships, destination);
    }

    public TraversalState onTripTowardsPlatform(TraversalStateType towardsState, RouteStationStateOnTrip routeStationStateOnTrip, GraphNode node,
                                                Duration cost, GraphTransaction txn) {
        final TraversalOps traversalOps = routeStationStateOnTrip.traversalOps;
        final OptionalResourceIterator<ImmutableGraphRelationship> towardsDest = getTowardsDestination(traversalOps, node, txn);
        if (!towardsDest.isEmpty()) {
            return new PlatformState(routeStationStateOnTrip, towardsDest.stream(), node, cost, towardsState);
        }

        final Stream<ImmutableGraphRelationship> platformRelationships = getBoardsAndOthers(node, txn, true, traversalOps);

        // Cannot filter here as might be starting a new trip from this point, so need to 'go back' to the route station
        //Stream<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationStateOnTrip);
        return new PlatformState(routeStationStateOnTrip, platformRelationships, node, cost, towardsState);
    }

    Stream<ImmutableGraphRelationship> getBoardsAndOthers(final GraphNode node, final GraphTransaction txn, boolean isPlatform, TraversalOps traversalOps) {

        final Stream<ImmutableGraphRelationship> other;
        if (isPlatform) {
            other = node.getRelationships(txn, OUTGOING, LEAVE_PLATFORM);
        } else {
            other = node.getRelationships(txn, OUTGOING, WALKS_FROM_STATION, NEIGHBOUR, GROUPED_TO_PARENT);
        }

        // todo need to order boarding relationships && remove service based prioritisation from JustBoarded State
        final Stream<ImmutableGraphRelationship> unsorted = node.getRelationships(txn, OUTGOING, BOARD, INTERCHANGE_BOARD);

        // order matters here, i.e. explore walks first TODO WHY?
        final Stream<ImmutableGraphRelationship> boarding = traversalOps.orderBoardingRelationsByRouteConnections(unsorted);
        return Stream.concat(other, boarding);
    }

    private OptionalResourceIterator<ImmutableGraphRelationship> getTowardsDestination(final TraversalOps traversalOps,
                                                                                       final GraphNode node, final GraphTransaction txn) {
        return traversalOps.getTowardsDestination(node.getRelationships(txn, OUTGOING, LEAVE_PLATFORM));
    }

    private NoPlatformStationState createNoPlatformStationState(TraversalState parentState, GraphNode node,
                                                                Duration cost, JourneyStateUpdate journeyState,
                                                                Stream<ImmutableGraphRelationship> relationships,
                                                                TraversalStateType towardsState) {
        return new NoPlatformStationState(parentState, relationships, cost, node, journeyState, towardsState);
    }

    private static PlatformState createPlatformState(TraversalStateType towardsState, RouteStationStateEndTrip routeStationState,
                                                     GraphNode node, Duration cost, Stream<ImmutableGraphRelationship> platformRelationships) {
        return new PlatformState(routeStationState, platformRelationships, node, cost, towardsState);
    }
}
