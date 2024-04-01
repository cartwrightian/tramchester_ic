package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.TraversalOps;

import java.time.Duration;
import java.util.EnumSet;
import java.util.stream.Stream;

public abstract class TraversalState extends EmptyTraversalState implements ImmutableTraversalState, NodeId {

    // GraphState -> JourneyState -> TraversalState

    protected final TraversalStateFactory traversalStateFactory;
    private final TraversalOps traversalOps;
    protected final GraphTransaction txn;

    private final Stream<ImmutableGraphRelationship> outbounds;
    private final Duration costForLastEdge;
    private final Duration parentCost;
    private final GraphNodeId graphNodeId;

    // initial only, at beginning of search
    protected TraversalState(final TraversalOps traversalOps, final TraversalStateFactory traversalStateFactory,
                             final TraversalStateType stateType, final GraphNodeId graphNodeId,
                             final GraphTransaction txn) {
        super(stateType);
        this.traversalOps = traversalOps;
        this.txn = txn;
        this.traversalStateFactory = traversalStateFactory;

        this.graphNodeId = graphNodeId;
        this.costForLastEdge = Duration.ZERO;
        this.parentCost = Duration.ZERO;
        this.outbounds = Stream.empty();
        if (stateType!=TraversalStateType.NotStartedState) {
            throw new RuntimeException("Attempt to create for incorrect initial state " + stateType);
        }
    }

    protected TraversalState(final ImmutableTraversalState parent, final Stream<ImmutableGraphRelationship> outbounds, final Duration costForLastEdge,
                             final TraversalStateType stateType, final GraphNodeId graphNodeId) {
        super(stateType);
        this.traversalOps = parent.getTraversalOps();
        this.txn = parent.getTransaction();
        this.traversalStateFactory = parent.getTraversalStateFactory();

        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalDuration();

        this.graphNodeId = graphNodeId;
    }

    @Override
    public TraversalStateFactory getTraversalStateFactory() {
        return traversalStateFactory;
    }

    @Override
    public GraphTransaction getTransaction() {
        return txn;
    }

    @Override
    public TraversalOps getTraversalOps() {
        return traversalOps;
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
        return graphNodeId;
    }

    public TraversalState nextState(final EnumSet<GraphLabel> nodeLabels, final GraphNode node,
                                    final JourneyStateUpdate journeyState, final Duration cost) {

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

        final TraversalStateType nextType = getNextStateType(stateType, actualNodeType, hasPlatforms, node, journeyState);

        return getTraversalState(nextType, node, journeyState, cost, isInterchange);

    }

    private TraversalState getTraversalState(final TraversalStateType nextType, final GraphNode node, final JourneyStateUpdate journeyStateUpdate,
                                             final Duration cost, final boolean isInterchange) {
        switch (nextType) {
            case MinuteState -> {
                return toMinute(traversalStateFactory.getTowardsMinute(stateType), node, cost, journeyStateUpdate);
            }
            case HourState -> {
                return toHour(traversalStateFactory.getTowardsHour(stateType), node, cost);
            }
            case GroupedStationState -> {
                return toGrouped(traversalStateFactory.getTowardsGroup(stateType), journeyStateUpdate, node, cost, journeyStateUpdate);
            }
            case PlatformStationState -> {
                return toPlatformStation(traversalStateFactory.getTowardsStation(stateType), node, cost, journeyStateUpdate);
            }
            case NoPlatformStationState -> {
                return toNoPlatformStation(traversalStateFactory.getTowardsNoPlatformStation(stateType), node, cost, journeyStateUpdate);
            }
            case ServiceState -> {
                return toService(traversalStateFactory.getTowardsService(stateType), node, cost);
            }
            case PlatformState -> {
                return toPlatform(traversalStateFactory.getTowardsPlatform(stateType), node, cost, journeyStateUpdate);
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
                                                final boolean hasPlatforms, final GraphNode node, JourneyStateUpdate journeyState) {
        switch (graphLabel) {
            case MINUTE -> { return TraversalStateType.MinuteState; }
            case HOUR -> { return TraversalStateType.HourState; }
            case GROUPED -> { return TraversalStateType.GroupedStationState; }
            case STATION -> { return hasPlatforms ? TraversalStateType.PlatformStationState : TraversalStateType.NoPlatformStationState; }
            case SERVICE -> { return TraversalStateType.ServiceState; }
            case PLATFORM -> { return TraversalStateType.PlatformState; }
            case QUERY_NODE -> { return TraversalStateType.WalkingState; }
            case ROUTE_STATION -> { return getRouteStationStateFor(currentStateType, node, journeyState); }
            default -> throw new RuntimeException("Unexpected at " + this + " label:" + graphLabel);
        }
    }

    private TraversalStateType getRouteStationStateFor(final TraversalStateType currentStateType, final GraphNode routeStationNode,
                                                       JourneyStateUpdate journeyState) {
        if (currentStateType==TraversalStateType.PlatformState || currentStateType==TraversalStateType.NoPlatformStationState) {
            return TraversalStateType.JustBoardedState;
        }

        if (currentStateType==TraversalStateType.MinuteState) {

            IdFor<Trip> tripId = journeyState.getCurrentTrip();

            if (traversalOps.hasOutboundTripFor(routeStationNode, tripId)) {
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

    public Duration getTotalDuration() {
        return parentCost.plus(getCurrentDuration());
    }

    public Duration getCurrentDuration() {
        return costForLastEdge;
    }

    protected TramTime getTimeFrom(final GraphNode graphNode) {
        return traversalOps.getTimeFrom(graphNode);
    }

    protected Trip getTrip(final IdFor<Trip> tripId) {
        return traversalOps.getTrip(tripId);
    }


    @Override
    public String toString() {
        return "TraversalState{" +
                "costForLastEdge=" + costForLastEdge +
                "nodeId=" + graphNodeId +
                ", parentCost=" + parentCost + System.lineSeparator() +
                '}';
    }
}
