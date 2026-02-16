package com.tramchester.graph.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

// marker
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public interface GraphRelationshipId extends GraphId {
}
