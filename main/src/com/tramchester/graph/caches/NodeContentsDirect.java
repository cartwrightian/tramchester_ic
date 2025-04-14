package com.tramchester.graph.caches;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;

import java.time.Duration;

// KEEP for assisting with debugging
@SuppressWarnings("unused")
public class NodeContentsDirect implements NodeContentsRepository {

    @Override
    public TramTime getTime(GraphNode node) {
        return node.getTime();
    }

    @Override
    public IdFor<Trip> getTripId(GraphRelationship relationship) {
        return relationship.getTripId();
    }

    @Override
    public Duration getCost(GraphRelationship relationship) {
        return relationship.getCost();
    }

    @Override
    public void deleteFromCostCache(GraphRelationship relationship) {
        // no-op
    }
}
