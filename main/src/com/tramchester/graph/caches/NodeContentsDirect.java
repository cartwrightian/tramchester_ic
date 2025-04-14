package com.tramchester.graph.caches;

import com.tramchester.graph.facade.GraphRelationship;

import java.time.Duration;

// KEEP for assisting with debugging
@SuppressWarnings("unused")
public class NodeContentsDirect implements NodeContentsRepository {

    @Override
    public Duration getCost(GraphRelationship relationship) {
        return relationship.getCost();
    }

    @Override
    public void deleteFromCostCache(GraphRelationship relationship) {
        // no-op
    }
}
