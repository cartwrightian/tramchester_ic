package com.tramchester.graph.search;

import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

public interface NumberOfNodesAndRelationshipsRepository {
    Long numberOf(GraphLabel label);

    long numberOf(TransportRelationshipTypes relationshipType);
}
