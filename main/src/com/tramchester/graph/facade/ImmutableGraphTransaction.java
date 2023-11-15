package com.tramchester.graph.facade;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;

import java.util.List;
import java.util.stream.Stream;

public class ImmutableGraphTransaction implements GraphTransaction  {

    private final MutableGraphTransaction underlying;

    public ImmutableGraphTransaction(MutableGraphTransaction underlying) {
        this.underlying = underlying;
    }

    @Override
    public void close() {
        underlying.close();
    }

    @Override
    public ImmuableGraphNode getNodeById(GraphNodeId nodeId) {
        return underlying.getNodeById(nodeId);
    }

    @Override
    public Stream<ImmuableGraphNode> findNodes(GraphLabel graphLabel) {
        return underlying.findNodes(graphLabel);
    }

    @Override
    public boolean hasAnyMatching(GraphLabel label, String field, String value) {
        return underlying.hasAnyMatching(label, field, value);
    }

    @Override
    public boolean hasAnyMatching(GraphLabel graphLabel) {
        return underlying.hasAnyMatching(graphLabel);
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmuableGraphNode findNode(ITEM item) {
        return underlying.findNode(item);
    }

    @Override
    public EvaluationContext createEvaluationContext(GraphDatabaseService databaseService) {
        return underlying.createEvaluationContext(databaseService);
    }

    @Override
    public List<GraphRelationship> getRouteStationRelationships(RouteStation routeStation, Direction direction) {
        return underlying.getRouteStationRelationships(routeStation, direction);
    }

    @Override
    public Iterable<ImmuableGraphNode> iter(Iterable<Node> iterable) {
        return underlying.iter(iterable);
    }

    @Override
    public GraphRelationship wrapRelationship(Relationship relationship) {
        return underlying.wrapRelationship(relationship);
    }

    @Override
    public GraphNode wrapNode(Node node) {
        return underlying.wrapNode(node);
    }

    @Override
    public GraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId) {
        return underlying.getRelationshipById(graphRelationshipId);
    }

    @Override
    public ImmuableGraphNode fromEnd(Path path) {
        return underlying.fromEnd(path);
    }

    @Override
    public GraphRelationship lastFrom(Path path) {
        return underlying.lastFrom(path);
    }

    @Override
    public ImmuableGraphNode fromStart(Path path) {
        return underlying.fromStart(path);
    }

    @Override
    public GraphNodeId createNodeId(Node node) {
        return underlying.createNodeId(node);
    }
}
