package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.GraphNode;
import com.tramchester.graph.search.ImmutableJourneyState;
import org.neo4j.graphdb.Path;

public class HowIGotHere {

    private static final long AT_START = Long.MIN_VALUE;
    private final long relationshipId;
    private final long nodeId;
    private final String traversalStateName;

    public HowIGotHere(Path path, ImmutableJourneyState immutableJourneyState, GraphNode graphNode) {
        this(graphNode.getId(), getRelationshipFromPath(path), immutableJourneyState.getTraversalStateName());
    }

    private HowIGotHere(long nodeId, long relationshipId, String traversalStateName) {
        this.nodeId = nodeId;
        this.relationshipId = relationshipId;
        this.traversalStateName = traversalStateName;
    }

    public long getEndNodeId() {
        return nodeId;
    }

    public long getRelationshipId() {
        return relationshipId;
    }

    public boolean atStart() {
        return relationshipId==AT_START;
    }

    public String getTraversalStateName() {
        return traversalStateName;
    }

    private static long getRelationshipFromPath(Path path) {
        if (path.lastRelationship()==null) {
            return AT_START;
        } else {
            return path.lastRelationship().getId();
        }
    }


    public static HowIGotHere forTest(long nodeId, long relationshipId) {
        return new HowIGotHere(nodeId, relationshipId, "TEST_ONLY");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HowIGotHere that = (HowIGotHere) o;

        if (relationshipId != that.relationshipId) return false;
        return nodeId == that.nodeId;
    }

    @Override
    public int hashCode() {
        int result = (int) (relationshipId ^ (relationshipId >>> 32));
        result = 31 * result + (int) (nodeId ^ (nodeId >>> 32));
        return result;
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
