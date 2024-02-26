package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public abstract class TraversalState extends EmptyTraversalState implements ImmutableTraversalState, NodeId {

    protected final TraversalStateFactory traversalStateFactory;
    protected final TraversalOps traversalOps;
    protected final GraphTransaction txn;

    private final Stream<ImmutableGraphRelationship> outbounds;
    private final Duration costForLastEdge;
    private final Duration parentCost;
    private final TraversalState parent;
    private final GraphNode graphNode;

    // only follow GOES_TO links for requested transport modes
    private final TransportRelationshipTypes[] requestedRelationshipTypes;

    private TraversalState child;

    // initial only
    protected TraversalState(final TraversalOps traversalOps, final TraversalStateFactory traversalStateFactory,
                             final EnumSet<TransportMode> requestedModes, final TraversalStateType stateType, GraphNode graphNode) {
        super(stateType);
        this.traversalOps = traversalOps;
        this.txn = traversalOps.getTransaction();
        this.traversalStateFactory = traversalStateFactory;
        this.requestedRelationshipTypes = TransportRelationshipTypes.forModes(requestedModes);
        this.graphNode = graphNode;

        this.costForLastEdge = Duration.ZERO;
        this.parentCost = Duration.ZERO;
        this.parent = null;
        this.outbounds = Stream.empty();
        if (stateType!=TraversalStateType.NotStartedState) {
            throw new RuntimeException("Attempt to create for incorrect initial state " + stateType);
        }
    }

    protected TraversalState(final TraversalState parent, final Stream<ImmutableGraphRelationship> outbounds, final Duration costForLastEdge,
                             final TraversalStateType stateType, GraphNode graphNode) {
        super(stateType);
        this.traversalOps = parent.traversalOps;
        this.txn = traversalOps.getTransaction();
        this.traversalStateFactory = parent.traversalStateFactory;
        this.parent = parent;

        this.outbounds = outbounds; //outbounds.toList();
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalDuration();

        this.requestedRelationshipTypes = parent.requestedRelationshipTypes;
        this.graphNode = graphNode;
    }

    public Stream<ImmutableGraphRelationship> getOutbounds(GraphTransaction txn) {
        return outbounds;
    }

    @Override
    public TraversalStateType getStateType() {
        return stateType;
    }

    @Override
    public GraphNodeId nodeId() {
        return graphNode.getId();
    }

    public static Stream<ImmutableGraphRelationship> getRelationships(final GraphTransaction txn, final GraphNode node,
                                                                      final Direction direction, final TransportRelationshipTypes types) {
        return node.getRelationships(txn, direction, types);
    }

    public TraversalState nextState(final EnumSet<GraphLabel> nodeLabels, final GraphNode node,
                                    final JourneyStateUpdate journeyState, final Duration cost, final boolean alreadyOnDiversion) {

        final boolean isInterchange = nodeLabels.contains(GraphLabel.INTERCHANGE);
        final boolean hasPlatforms = nodeLabels.contains(GraphLabel.HAS_PLATFORMS);

        final GraphLabel actualNodeType;
        final int numLabels = nodeLabels.size();
        if (numLabels==1) {
            actualNodeType = nodeLabels.iterator().next();
        } else if (numLabels==3 && isInterchange && nodeLabels.contains(GraphLabel.ROUTE_STATION)) {
            actualNodeType = GraphLabel.ROUTE_STATION;
        } else if (numLabels==2 && nodeLabels.contains(GraphLabel.ROUTE_STATION)) {
            actualNodeType = GraphLabel.ROUTE_STATION;
        } else if (nodeLabels.contains(GraphLabel.STATION)) {
            actualNodeType = GraphLabel.STATION;
        } else if (nodeLabels.contains(GraphLabel.HOUR)) {
            actualNodeType = GraphLabel.HOUR;
        } else {
            throw new RuntimeException("Not a station, unexpected multi-label condition: " + nodeLabels);
        }

        final TraversalStateType nextType = getNextStateType(stateType, actualNodeType, hasPlatforms, node);

        return getTraversalState(nextType, node, journeyState, cost, alreadyOnDiversion, isInterchange);

    }

    private TraversalState getTraversalState(final TraversalStateType nextType, final GraphNode node, final JourneyStateUpdate journeyStateUpdate,
                                             final Duration cost, final boolean alreadyOnDiversion, final boolean isInterchange) {
        switch (nextType) {
            case MinuteState -> {
                return toMinute(traversalStateFactory.getTowardsMinute(stateType), node, cost, journeyStateUpdate, requestedRelationshipTypes);
            }
            case HourState -> {
                return toHour(traversalStateFactory.getTowardsHour(stateType), node, cost);
            }
            case GroupedStationState -> {
                return toGrouped(traversalStateFactory.getTowardsGroup(stateType), journeyStateUpdate, node, cost, journeyStateUpdate);
            }
            case PlatformStationState -> {
                return toPlatformStation(traversalStateFactory.getTowardsStation(stateType), node, cost, journeyStateUpdate, alreadyOnDiversion);
            }
            case NoPlatformStationState -> {
                return toNoPlatformStation(traversalStateFactory.getTowardsNoPlatformStation(stateType), node, cost, journeyStateUpdate, alreadyOnDiversion);
            }
            case ServiceState -> {
                return toService(traversalStateFactory.getTowardsService(stateType), node, cost);
            }
            case PlatformState -> {
                return toPlatform(traversalStateFactory.getTowardsPlatform(stateType), node, cost, alreadyOnDiversion, journeyStateUpdate);
            }
            case WalkingState -> {
                return toWalk(traversalStateFactory.getTowardsWalk(stateType), node, cost, journeyStateUpdate);
            }
            case RouteStationStateOnTrip -> {
                return toRouteStationOnTrip(traversalStateFactory.getTowardsRouteStationOnTrip(stateType), journeyStateUpdate, node, cost, isInterchange);
            }
            case RouteStationStateEndTrip -> {
                return toRouteStationEndTrip(traversalStateFactory.getTowardsRouteStationEndTrip(stateType), journeyStateUpdate, node, cost, isInterchange);
            }
            case JustBoardedState -> {
                return toJustBoarded(traversalStateFactory.getTowardsJustBoarded(stateType), node, cost, journeyStateUpdate);
            }
            default -> throw new RuntimeException("Unexpected next state " + nextType + " at " + this);
        }
    }

    private TraversalStateType getNextStateType(final TraversalStateType currentStateType, final GraphLabel graphLabel,
                                                final boolean hasPlatforms, final GraphNode node) {
        switch (graphLabel) {
            case MINUTE -> { return TraversalStateType.MinuteState; }
            case HOUR -> { return TraversalStateType.HourState; }
            case GROUPED -> { return TraversalStateType.GroupedStationState; }
            case STATION -> { return hasPlatforms ? TraversalStateType.PlatformStationState : TraversalStateType.NoPlatformStationState; }
            case SERVICE -> { return TraversalStateType.ServiceState; }
            case PLATFORM -> { return TraversalStateType.PlatformState; }
            case QUERY_NODE -> { return TraversalStateType.WalkingState; }
            case ROUTE_STATION -> { return getRouteStationStateFor(currentStateType, node); }
            default -> throw new RuntimeException("Unexpected at " + this + " label:" + graphLabel);
        }
    }

    private TraversalStateType getRouteStationStateFor(final TraversalStateType currentStateType, final GraphNode node) {
        if (currentStateType==TraversalStateType.PlatformState || currentStateType==TraversalStateType.NoPlatformStationState) {
            return TraversalStateType.JustBoardedState;
        }
        if (currentStateType==TraversalStateType.MinuteState) {
            final MinuteState minuteState = (MinuteState) this;
            if (traversalOps.hasOutboundFor(node, minuteState.getServiceId())) {
                return TraversalStateType.RouteStationStateOnTrip;
            } else {
                return TraversalStateType.RouteStationStateEndTrip;
            }
        } else {
            throw new RuntimeException("Unexpected from state " + currentStateType);
        }
    }

    public void toDestination(final TraversalState from, final GraphNode finalNode, final Duration cost, final JourneyStateUpdate journeyState) {
        toDestination(traversalStateFactory.getTowardsDestination(from.getStateType()), finalNode, cost, journeyState);
    }

    public void dispose() {
        if (child!=null) {
            child.dispose();
            child = null;
        }
    }

    protected static <R extends GraphRelationship> Stream<R> filterExcludingEndNode(final GraphTransaction txn,
                                                                                    final Stream<R> relationships,
                                                                                    final NodeId hasNodeId) {
        final GraphNodeId nodeId = hasNodeId.nodeId();
        return relationships.filter(relationship -> !relationship.getEndNodeId(txn).equals(nodeId));
    }

    public Duration getTotalDuration() {
        return parentCost.plus(getCurrentDuration());
    }

    public Duration getCurrentDuration() {
        return costForLastEdge;
    }

    @Override
    public String toString() {
        return "TraversalState{" +
                "costForLastEdge=" + costForLastEdge +
                "nodeId=" + graphNode.getId() +
                ", parentCost=" + parentCost + System.lineSeparator() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

    protected void board(JourneyStateUpdate journeyState, boolean hasPlatforms) throws TramchesterException {
        final TransportMode actualMode = graphNode.getTransportMode();
        if (actualMode==null) {
            throw new RuntimeException(format("Unable get transport mode at %s for %s", graphNode.getLabels(), graphNode.getAllProperties()));
        }
        journeyState.board(actualMode, graphNode, hasPlatforms);
    }
}
