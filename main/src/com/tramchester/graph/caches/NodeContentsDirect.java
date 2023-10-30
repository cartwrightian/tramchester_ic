package com.tramchester.graph.caches;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

// KEEP for assisting with debugging
@SuppressWarnings("unused")
public class NodeContentsDirect implements NodeContentsRepository {

    @Override
    public IdFor<RouteStation> getRouteStationId(Node node) {
        return GraphProps.getRouteStationIdFrom(node);
    }

    @Override
    public IdFor<Service> getServiceId(Node node) {
        return GraphProps.getServiceIdFrom(node);
    }

    @Override
    public IdFor<Trip> getTripId(Node node) {
        return GraphProps.getTripId(node);
    }

    @Override
    public TramTime getTime(Node node) {
        return GraphProps.getTime(node);
    }

    @Override
    public int getHour(Node node) {
        return GraphProps.getHour(node);
    }

    @Override
    public IdFor<Trip> getTripId(Relationship relationship) {
        return GraphProps.getTripId(relationship);
    }

    @Override
    public Duration getCost(Relationship relationship) {
        return GraphProps.getCost(relationship);
    }

    @Override
    public void deleteFromCostCache(GraphRelationship relationship) {
        // no-op
    }

    @Override
    public EnumSet<GraphLabel> getLabels(Node node) {
        final Set<GraphLabel> graphLabels = GraphLabel.from(node.getLabels());
        return EnumSet.copyOf(graphLabels);
    }

    @Override
    public EnumSet<GraphLabel> getLabels(GraphNode node) {
        final Set<GraphLabel> graphLabels = GraphLabel.from(node.getLabels());
        return EnumSet.copyOf(graphLabels);
    }
}
