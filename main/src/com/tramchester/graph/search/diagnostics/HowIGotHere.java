package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphRelationshipId;
import com.tramchester.graph.search.ImmutableJourneyState;

import java.util.Objects;

public class HowIGotHere {

    //private static final long AT_START = Long.MIN_VALUE;
    private final GraphRelationshipId relationshipId;
    private final GraphNodeId nodeId;
    private final String traversalStateName;

    public HowIGotHere(ImmutableJourneyState immutableJourneyState, GraphNode graphNode, GraphRelationship lastFrom) {
        this(graphNode.getId(), maintainExistingInterface(lastFrom), immutableJourneyState.getTraversalStateName());
    }

    // TODO For no presevre existing behaviour with relationshipId i.e. null means not started yet
    private static GraphRelationshipId maintainExistingInterface(GraphRelationship lastFrom) {
        if (lastFrom==null) {
            return null;
        }
        return lastFrom.getId();
    }

    private HowIGotHere(GraphNodeId nodeId, GraphRelationshipId relationshipId, String traversalStateName) {
        this.nodeId = nodeId;
        this.relationshipId = relationshipId;
        this.traversalStateName = traversalStateName;
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

    public String getTraversalStateName() {
        return traversalStateName;
    }

//    private static GraphRelationshipId getRelationshipFromPath(Path path) {
//        if (path.lastRelationship()==null) {
//            return null;
//        } else {
//            return GraphRelationship.lastFrom(path).getId(); // path.lastRelationship().getId();
//        }
//    }

    public static HowIGotHere forTest(GraphNodeId nodeId, GraphRelationshipId relationshipId) {
        return new HowIGotHere(nodeId, relationshipId, "TEST_ONLY");
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
                ", traversalStateName='" + traversalStateName + '\'' +
                '}';
    }

}
