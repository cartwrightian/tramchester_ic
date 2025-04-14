package com.tramchester.graph.caches;

import com.google.inject.ImplementedBy;
import com.tramchester.graph.facade.GraphRelationship;

import java.time.Duration;

@ImplementedBy(CachedNodeOperations.class)
public interface NodeContentsRepository  {

    Duration getCost(GraphRelationship lastRelationship);
    void deleteFromCostCache(GraphRelationship relationship);


}
