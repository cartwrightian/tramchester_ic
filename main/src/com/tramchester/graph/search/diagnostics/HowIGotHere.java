package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphRelationshipId;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.stateMachine.states.HasTowardsStationId;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;

import java.util.Objects;

public class HowIGotHere {

    private final GraphRelationshipId relationshipId;
    private final GraphNodeId nodeId;
    private final TraversalStateType traversalStateType;
    private final IdFor<? extends Location<?>> approxPosition;
    private final IdFor<Station> towards;

    public HowIGotHere(final ImmutableJourneyState immutableJourneyState, final GraphNode graphNode, final GraphRelationship lastFrom) {
        this(graphNode.getId(), maintainExistingInterface(lastFrom), immutableJourneyState.getTraversalStateType(),
                immutableJourneyState.approxPosition(), getTowards(immutableJourneyState));
    }

    public HowIGotHere(final GraphNodeId nodeId, final GraphRelationshipId relationshipId, final TraversalStateType traversalStateType,
                       IdFor<? extends Location<?>> approxPosition, IdFor<Station> towards) {
        this.nodeId = nodeId;
        this.relationshipId = relationshipId;
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
                    throw new RuntimeException("Missing towrards for " + traversalStateType);
                }
            }
            default -> Station.InvalidId();
        };

    }

    // TODO use state type instead?
    private static GraphRelationshipId maintainExistingInterface(final GraphRelationship lastFrom) {
        if (lastFrom==null) {
            return null;
        }
        return lastFrom.getId();
    }

    public GraphNodeId getEndNodeId() {
        return nodeId;
    }

    public GraphRelationshipId getRelationshipId() {
        return relationshipId;
    }

    public boolean atStart() {
        return relationshipId==null;
    }

    public TraversalStateType getTraversalStateType() {
        return traversalStateType;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HowIGotHere that = (HowIGotHere) o;
        return Objects.equals(relationshipId, that.relationshipId) && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationshipId, nodeId);
    }

    @Override
    public String toString() {
        return "HowIGotHere{" +
                "relationshipId=" + relationshipId +
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
}
