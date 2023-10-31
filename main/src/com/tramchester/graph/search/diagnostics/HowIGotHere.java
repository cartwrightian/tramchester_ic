package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphNodeId;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.GraphRelationshipId;
import com.tramchester.graph.search.ImmutableJourneyState;
import org.neo4j.graphdb.Path;

import java.util.Objects;

public class HowIGotHere {

    //private static final long AT_START = Long.MIN_VALUE;
    private final GraphRelationshipId relationshipId;
    private final GraphNodeId nodeId;
    private final String traversalStateName;

    public HowIGotHere(Path path, ImmutableJourneyState immutableJourneyState, GraphNode graphNode) {
        this(graphNode.getId(), getRelationshipFromPath(path), immutableJourneyState.getTraversalStateName());
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

    private static GraphRelationshipId getRelationshipFromPath(Path path) {
        if (path.lastRelationship()==null) {
            return null;
        } else {
            return GraphRelationship.lastFrom(path).getId(); // path.lastRelationship().getId();
        }
    }

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
