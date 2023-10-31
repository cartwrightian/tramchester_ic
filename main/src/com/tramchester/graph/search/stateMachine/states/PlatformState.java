package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.*;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformState extends TraversalState implements NodeId {

    public static class Builder implements Towards<PlatformState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.RouteStationStateOnTrip, this);
            registers.add(TraversalStateType.RouteStationStateEndTrip, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.PlatformState;
        }

        public PlatformState from(PlatformStationState platformStationState, GraphNode node, Duration cost) {
            // inc. board here since might be starting journey
            return new PlatformState(platformStationState,
                    node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD), node, cost, this);
        }

        public TraversalState fromRouteStationOnTrip(RouteStationStateOnTrip routeStationStateOnTrip, GraphNode node, Duration cost) {

            // towards final destination, just follow this one
            OptionalResourceIterator<GraphRelationship> towardsDest = getTowardsDestination(routeStationStateOnTrip.traversalOps, node);
            if (!towardsDest.isEmpty()) {
                return new PlatformState(routeStationStateOnTrip, towardsDest.stream(), node, cost, this);
            }

            // inc. board here since might be starting journey
            Stream<GraphRelationship> platformRelationships = node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);

            // Cannot filter here as might be starting a new trip from this point, so need to 'go back' to the route station
            //Stream<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationStateOnTrip);
            return new PlatformState(routeStationStateOnTrip, platformRelationships, node, cost, this);
        }

        public TraversalState fromRouteStatiomEndTrip(RouteStationStateEndTrip routeStationState, GraphNode node, Duration cost) {
            // towards final destination, just follow this one
            OptionalResourceIterator<GraphRelationship> towardsDest = getTowardsDestination(routeStationState.traversalOps, node);
            if (!towardsDest.isEmpty()) {
                return new PlatformState(routeStationState, towardsDest.stream(), node, cost, this);
            }

            Stream<GraphRelationship> platformRelationships = node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
            // end of a trip, may need to go back to this route station to catch new service
            return new PlatformState(routeStationState, platformRelationships, node, cost, this);
        }

        private OptionalResourceIterator<GraphRelationship> getTowardsDestination(TraversalOps traversalOps, GraphNode node) {
            return traversalOps.getTowardsDestination(node.getRelationships(OUTGOING, LEAVE_PLATFORM));
        }

    }

    private final GraphNode platformNode;

    private PlatformState(TraversalState parent, Stream<GraphRelationship> relationships, GraphNode platformNode, Duration cost, Towards<PlatformState> builder) {
        super(parent, relationships, cost, builder.getDestination());
        this.platformNode = platformNode;
    }

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNode.getIdOLD() +
                "} " + super.toString();
    }

    @Override
    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, GraphNode node, Duration cost, JourneyStateUpdate journeyState) {
        try {
            TransportMode actualMode = GraphProps.getTransportMode(node);
            if (actualMode==null) {
                throw new RuntimeException(format("Unable get transport mode at %s for %s", node.getLabels(), node.getAllProperties()));
            }
            journeyState.board(actualMode, platformNode, true);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board tram", e);
        }
        return towardsJustBoarded.fromPlatformState(this, node, cost);
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, GraphNode node, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        return towardsStation.fromPlatform(this, node, cost, journeyState, onDiversion);
    }

    @Override
    public long nodeId() {
        return platformNode.getIdOLD();
    }
}
