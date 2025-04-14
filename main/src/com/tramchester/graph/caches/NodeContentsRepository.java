package com.tramchester.graph.caches;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.time.Duration;
import java.util.EnumSet;

@ImplementedBy(CachedNodeOperations.class)
public interface NodeContentsRepository  {

    TramTime getTime(GraphNode node);
    EnumSet<GraphLabel> getLabels(GraphNode node);

    IdFor<Trip> getTripId(GraphRelationship relationship);
    Duration getCost(GraphRelationship lastRelationship);
    void deleteFromCostCache(GraphRelationship relationship);


}
