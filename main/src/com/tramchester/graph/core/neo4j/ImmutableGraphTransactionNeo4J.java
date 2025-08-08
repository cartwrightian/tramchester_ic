package com.tramchester.graph.core.neo4j;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasGraphLabel;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.*;

import java.util.List;
import java.util.stream.Stream;

public class ImmutableGraphTransactionNeo4J implements GraphTransaction, GraphTransactionNeo4J {

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
    public GraphNode getNodeById(final GraphNodeId nodeId) {
        return underlying.getNodeById(nodeId);
    }

    @Override
    public Stream<GraphNode> findNodes(final GraphLabel graphLabel) {
        return underlying.findNodes(graphLabel);
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel label, final GraphPropertyKey field, final String value) {
        return underlying.hasAnyMatching(label, field, value);
    }

    @Override
    public boolean hasAnyMatching(final GraphLabel graphLabel) {
        return underlying.hasAnyMatching(graphLabel);
    }

    @Override
    public <ITEM extends GraphProperty & HasGraphLabel & HasId<TYPE>, TYPE extends CoreDomain> GraphNode findNode(final ITEM item) {
        return underlying.findNode(item);
    }

    @Override
    public EvaluationContext createEvaluationContext(final GraphDatabaseService databaseService) {
        return underlying.createEvaluationContext(databaseService);
    }

    @Override
    public List<GraphRelationship> getRouteStationRelationships(final RouteStation routeStation, final GraphDirection direction) {
        return underlying.getRouteStationRelationships(routeStation, direction);
    }

    @Override
    public Iterable<GraphNode> iter(final Iterable<Node> iterable) {
        return underlying.iter(iterable);
    }

    @Override
    public ImmutableGraphRelationshipNeo4J wrapRelationship(final Relationship relationship) {
        return underlying.wrapRelationship(relationship);
    }

    @Override
    public GraphNode wrapNode(final Node node) {
        return underlying.wrapNode(node);
    }

    @Override
    public ImmutableGraphRelationshipNeo4J getRelationshipById(final GraphRelationshipId graphRelationshipId) {
        return underlying.getRelationshipById(graphRelationshipId);
    }

    @Override
    public GraphNode fromEnd(final Path path) {
        return underlying.fromEnd(path);
    }

    @Override
    public GraphNode fromStart(final Path path) {
        return underlying.fromStart(path);
    }

    @Override
    public GraphNode getStartNode(final Relationship relationship) {
        return underlying.getStartNode(relationship);
    }

    @Override
    public GraphNode getEndNode(final Relationship relationship) {
        return underlying.getEndNode(relationship);
    }

    @Override
    public GraphNodeId getStartNodeId(final Relationship relationship) {
        return underlying.getStartNodeId(relationship);
    }

    @Override
    public GraphNodeId getEndNodeId(final Relationship relationship) {
        return underlying.getEndNodeId(relationship);
    }

    @Override
    public GraphNodeId getGraphIdFor(Node node) {
        return underlying.getGraphIdFor(node);
    }


}
