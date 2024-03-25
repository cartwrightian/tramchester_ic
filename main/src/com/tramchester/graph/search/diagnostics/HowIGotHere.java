package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.stateMachine.states.HasTowardsStationId;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;

import java.util.Objects;

public class HowIGotHere {

    private final GraphNodeId nodeId;

    private final TraversalStateType traversalStateType;
    private final IdFor<? extends Location<?>> approxPosition;
    private final IdFor<Station> towards;
    private final GraphNodeId previousId;

    public HowIGotHere(final ImmutableJourneyState immutableJourneyState, final GraphNodeId endNodeId, final GraphNodeId previousNodeId) {
        this(endNodeId, previousNodeId, immutableJourneyState.getTraversalStateType(),
                immutableJourneyState.approxPosition(), getTowards(immutableJourneyState));
    }

    public HowIGotHere(final GraphNodeId endNodeId, final GraphNodeId previousNodeId, final TraversalStateType traversalStateType,
                       IdFor<? extends Location<?>> approxPosition, final IdFor<Station> towards) {
        this.nodeId = endNodeId;
        this.previousId = previousNodeId;
        this.traversalStateType = traversalStateType;
        this.approxPosition = approxPosition;
        this.towards = towards;
    }

    private static IdFor<Station> getTowards(final ImmutableJourneyState journeyState) {
        final TraversalStateType traversalStateType = journeyState.getTraversalStateType();
        return switch (traversalStateType) {
            case ServiceState, MinuteState, HourState -> {
                final ImmutableTraversalState traversalState = journeyState.getTraversalState();
                if (traversalState instanceof HasTowardsStationId hasTowardsStationId) {
                    yield hasTowardsStationId.getTowards();
                } else {
                    throw new RuntimeException("Missing towards for " + traversalStateType);
                }
            }
            default -> Station.InvalidId();
        };

    }

    public GraphNodeId getEndNodeId() {
        return nodeId;
    }

    public boolean atStart() {
        return previousId==null;
    }

    public TraversalStateType getTraversalStateType() {
        return traversalStateType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HowIGotHere that = (HowIGotHere) o;
        return Objects.equals(previousId, that.previousId) && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(previousId, nodeId);
    }

    @Override
    public String toString() {
        return "HowIGotHere{" +
                ", nodeId=" + nodeId +
                ", traversalStateName=" + traversalStateType +
                ", approxPosition=" + approxPosition +
                '}';
    }

    public IdFor<? extends Location<?>> getApproxLocation() {
        return approxPosition;
    }

    public boolean hasTowardsId() {
        return towards.isValid();
    }

    public IdFor<Station> getTowardsId() {
        return towards;
    }

    public GraphNodeId getPreviousId() {
        return previousId;
    }
}
