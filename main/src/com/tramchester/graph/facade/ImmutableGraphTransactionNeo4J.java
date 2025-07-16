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

public class ImmutableGraphTransactionNeo4J implements GraphTransaction, GraphTransactionNeo4J, GraphTraverseTransaction  {

    private final MutableGraphTransactionNeo4J underlying;

    public ImmutableGraphTransactionNeo4J(final MutableGraphTransactionNeo4J underlying) {
        this.underlying = underlying;
    }

    @Override
    public int getTransactionId() {
        return underlying.getTransactionId();
    }

    @Override
    public void close() {
        underlying.close();
    }

    @Override
    public ImmutableGraphNode getNodeById(final GraphNodeId nodeId) {
        return underlying.getNodeById(nodeId);
    }

    @Override
    public Stream<ImmutableGraphNode> findNodes(final GraphLabel graphLabel) {
        return underlying.findNodes(graphLabel);
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel label, final String field, final String value) {
        return underlying.hasAnyMatching(label, field, value);
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel graphLabel) {
        return underlying.hasAnyMatching(graphLabel);
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> ImmutableGraphNode findNode(final ITEM item) {
        return underlying.findNode(item);
    }

    @Override
    public EvaluationContext createEvaluationContext(final GraphDatabaseService databaseService) {
        return underlying.createEvaluationContext(databaseService);
    }

    @Override
    public List<ImmutableGraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final GraphDirection direction) {
        return underlying.getRouteStationRelationships(routeStation, direction);
    }

    @Override
    public Iterable<ImmutableGraphNode> iter(final Iterable<Node> iterable) {
        return underlying.iter(iterable);
    }

    @Override
    public ImmutableGraphRelationship wrapRelationship(final Relationship relationship) {
        return underlying.wrapRelationship(relationship);
    }

    @Override
    public GraphNode wrapNode(final Node node) {
        return underlying.wrapNode(node);
    }

    @Override
    public ImmutableGraphRelationship getRelationshipById(final GraphRelationshipId graphRelationshipId) {
        return underlying.getRelationshipById(graphRelationshipId);
    }

    @Override
    public ImmutableGraphNode fromEnd(final Path path) {
        return underlying.fromEnd(path);
    }

    @Override
    public ImmutableGraphRelationship lastFrom(final Path path) {
        return underlying.lastFrom(path);
    }

    @Override
    public GraphNodeId getPreviousNodeId(final Path path) {
        return underlying.getPreviousNodeId(path);
    }

    @Override
    public ImmutableGraphNode fromStart(final Path path) {
        return underlying.fromStart(path);
    }

    @Override
    public ImmutableGraphNode getStartNode(final Relationship relationship) {
        return underlying.getStartNode(relationship);
    }

    @Override
    public ImmutableGraphNode getEndNode(final Relationship relationship) {
        return underlying.getEndNode(relationship);
    }

    @Override
    public GraphNodeId endNodeNodeId(final Path path) {
        return underlying.endNodeNodeId(path);
    }

    @Override
    public GraphNodeId getStartNodeId(final Relationship relationship) {
        return underlying.getStartNodeId(relationship);
    }

    @Override
    public GraphNodeId getEndNodeId(final Relationship relationship) {
        return underlying.getEndNodeId(relationship);
    }


}
