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
    public ImmutableGraphNode getNodeById(GraphNodeId nodeId) {
        return underlying.getNodeById(nodeId);
    }

    @Override
    public Stream<ImmutableGraphNode> findNodes(GraphLabel graphLabel) {
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
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmutableGraphNode findNode(ITEM item) {
        return underlying.findNode(item);
    }

    @Override
    public EvaluationContext createEvaluationContext(GraphDatabaseService databaseService) {
        return underlying.createEvaluationContext(databaseService);
    }

    @Override
    public List<ImmutableGraphRelationship> getRouteStationRelationships(RouteStation routeStation, Direction direction) {
        return underlying.getRouteStationRelationships(routeStation, direction);
    }

    @Override
    public Iterable<ImmutableGraphNode> iter(Iterable<Node> iterable) {
        return underlying.iter(iterable);
    }

    @Override
    public ImmutableGraphRelationship wrapRelationship(Relationship relationship) {
        return underlying.wrapRelationship(relationship);
    }

    @Override
    public GraphNode wrapNode(Node node) {
        return underlying.wrapNodeAsImmutable(node);
    }

    @Override
    public ImmutableGraphRelationship getRelationshipById(GraphRelationshipId graphRelationshipId) {
        return underlying.getRelationshipById(graphRelationshipId);
    }

    @Override
    public ImmutableGraphNode fromEnd(Path path) {
        return underlying.fromEnd(path);
    }

    @Override
    public ImmutableGraphRelationship lastFrom(Path path) {
        return underlying.lastFrom(path);
    }

    @Override
    public ImmutableGraphNode fromStart(Path path) {
        return underlying.fromStart(path);
    }

    @Override
    public GraphNodeId createNodeId(Node node) {
        return underlying.createNodeId(node);
    }

    @Override
    public ImmutableGraphNode getStartNode(Relationship relationship) {
        return underlying.getStartNode(relationship);
    }

    @Override
    public ImmutableGraphNode getEndNode(Relationship relationship) {
        return underlying.getEndNode(relationship);
    }
}
