package com.tramchester.graph.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Map;

public interface GraphEntity<T extends GraphId> {
    @JsonIgnore
    boolean isNode();

    @JsonIgnore
    boolean isRelationship();

    Map<GraphPropertyKey, Object> getAllProperties();

    T getId();

}
